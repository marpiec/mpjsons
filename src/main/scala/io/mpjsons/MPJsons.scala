package io.mpjsons

import io.mpjsons.impl.serializer.{Utf8Output, Utf8Writer}
import io.mpjsons.impl.special.TypeNames
import io.mpjsons.impl.special.TypedConverter.{TypedDeserializer, TypedSerializer}
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, JsonInnerException, SerializerFactory, StringIterator}

import java.nio.charset.StandardCharsets
import java.util.concurrent.ConcurrentHashMap
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.universe._


private object ErrorMessageFormatter {

  private final val trimmingSize = 40

  /**
   * Describes a deserialization failure with only a short window of the input around the failing position, so the
   * message stays small and does not copy whole (possibly sensitive) payloads into logs.
   */
  def formatDeserializationError(json: String, jsonIterator: StringIterator, e: RuntimeException, tpe: Type): String = {
    val input = jsonIterator.stringValue
    val position = Math.min(Math.max(jsonIterator.nextIndex - 1, 0), input.length)
    val start = Math.max(0, position - trimmingSize)
    val end = Math.min(input.length, position + trimmingSize)
    val prefix = if (start > 0) "..." else ""
    val suffix = if (end < input.length) "..." else ""
    val window = printable(input.substring(start, end))
    val caret = " " * (prefix.length + position - start) + "^"
    s"""|Problem deserializing: $tpe
        |Problem: ${e.toString}
        |At position $position of ${input.length}:
        |$prefix$window$suffix
        |$caret""".stripMargin
  }

  /**
   * Input nested deeper than the stack allows (without `MPJsonsConfig.maxDepth`) is reported like any other
   * malformed input instead of letting a StackOverflowError escape.
   */
  def tooDeep(jsonIterator: StringIterator, e: StackOverflowError, tpe: Type): JsonInnerException = {
    val cause = new IllegalArgumentException("JSON nesting is too deep for the thread stack; set MPJsonsConfig(maxDepth = ...) to limit it", e)
    new JsonInnerException(formatDeserializationError(jsonIterator.stringValue, jsonIterator, cause, tpe), cause)
  }

  /** Keeps the window on one line so the caret lines up. */
  private def printable(text: String): String = {
    text.map(c => if (c < ' ') ' ' else c)
  }
}

/**
 * Remembers the length of the last output produced for one type, so the next output buffer can be
 * allocated at the right size instead of growing from the default capacity in several steps.
 * Races between threads only make the hint slightly stale, which is harmless.
 */
private[mpjsons] final class OutputSizeHint {
  @volatile private var length: Int = 64

  // Margin: StringBuilder.append(double) reserves 24 chars up front (JDK 19+), so a trailing number must not overflow.
  def newBuilder(): StringBuilder = new StringBuilder(length + 32)

  def update(builder: StringBuilder): Unit = {
    length = builder.length
  }

  @volatile private var byteLength: Int = 64

  def newUtf8Output(): Utf8Output = new Utf8Output(byteLength + 32)

  def update(out: Utf8Output): Unit = {
    byteLength = out.size
  }
}

/**
 * Serializer for a single type.
 */
class StaticSerializer[T](private val innerSerializer: JsonTypeSerializer[T], config: MPJsonsConfig) {

  def this(innerSerializer: JsonTypeSerializer[T]) = this(innerSerializer, MPJsonsConfig())

  private val sizeHint = new OutputSizeHint

  /**
   * Takes an object and returns a String containing JSON representation of that object.
   */
  def serialize(obj: T): String = {
    serializeToStringBuilder(obj).toString()
  }

  /**
   * Takes an object and a StringBuilder and puts JSON representation of that object into the Builder.
   * Useful when StringBuilder can be used without converting it to a String
   * which requires copying of underlying char array.
   */
  def serializeToStringBuilder(obj: T): StringBuilder = {
    val json = sizeHint.newBuilder()
    if (obj == null) NullValues.writeNull(json, config.nullPolicy != NullPolicy.Fail, "the top-level value") else innerSerializer.serialize(obj, json)
    sizeHint.update(json)
    json
  }

  private val utf8Writer: Utf8Writer[T] = Utf8Writer.of(innerSerializer)

  /**
   * Takes an object and returns JSON representation of that object encoded in UTF-8: the same bytes as
   * `serialize(obj).getBytes(UTF_8)`, written without building the String first.
   */
  def serializeToBytes(obj: T): Array[Byte] = {
    if (obj == null) {
      serialize(obj).getBytes(StandardCharsets.UTF_8)
    } else {
      val out = sizeHint.newUtf8Output()
      utf8Writer.writeUtf8(obj, out)
      sizeHint.update(out)
      out.toByteArray
    }
  }
}

/**
 * Deserializer for a single type.
 */
class StaticDeserializer[T](private val innerDeserializer: JsonTypeDeserializer[T], tpe: Type, config: MPJsonsConfig) {

  def this(innerDeserializer: JsonTypeDeserializer[T], tpe: Type) = this(innerDeserializer, tpe, MPJsonsConfig())

  /**
   * Takes a String containing JSON representation of objects and returns an instance of given object.
   */
  def deserialize(json: String): T = {
    val jsonIterator = new StringIterator(json, config.strict)
    try {
      val result = if (jsonIterator.tryConsumeNull()) NullValues.forType(tpe, config).get().asInstanceOf[T] else innerDeserializer.deserialize(jsonIterator)
      jsonIterator.expectEnd()
      result
    } catch {
      case e: RuntimeException =>
        throw new JsonInnerException(ErrorMessageFormatter.formatDeserializationError(json, jsonIterator, e, tpe), e)
      case e: StackOverflowError =>
        throw ErrorMessageFormatter.tooDeep(jsonIterator, e, tpe)
    }
  }

}

/**
  * Allows field in bean to be ommited if it's value is null
  */
final class nullable extends StaticAnnotation

/**
 * Main class of json serialization library.
 * @author Marcin Pieciukiewicz
 */
class MPJsons(val config: MPJsonsConfig) {

  /** Backward compatible constructor; `ignoreNullFields = true` is the same as `NullPolicy.Omit`. */
  def this(ignoreNonExistingFields: Boolean = false, ignoreNullFields: Boolean = false, nullPolicy: NullPolicy = NullPolicy.Fail) =
    this(MPJsonsConfig(ignoreNonExistingFields = ignoreNonExistingFields, nullPolicy = if (ignoreNullFields) NullPolicy.Omit else nullPolicy))

  private val deserializerFactory = new DeserializerFactory(config)
  private val serializerFactory = new SerializerFactory(config)

  private val extractTypeFromNameCache = new ConcurrentHashMap[String, Type]()
  private val outputSizeHints = new ConcurrentHashMap[Type, OutputSizeHint]()

  private val nothingTypeTag = typeTag[Nothing]

  private val rootContext = Context(List.empty, Map.empty)

  // Scala 2.13 (since 2.13.7) returns the same TypeTag for every materialisation at a call site (cached per
  // TypeCreator class in the runtime mirror), so `tag.tpe` is computed once; a cache here would only add a lookup.
  private def extractType[T](tag: TypeTag[T]): Type = tag.tpe

  // Per-call paths test the resolved type: comparing tags (`tag == nothingTypeTag`) compares mirrors and types.
  private def isNothing(tpe: Type): Boolean = tpe.typeSymbol == definitions.NothingClass

  private def extractTypeFromName[T](typeName: String): Type = {
    val cached = extractTypeFromNameCache.get(typeName)
    if (cached != null) {
      cached
    } else {
      val tpe = TypesUtil.getTypeFromClass(Class.forName(typeName))
      extractTypeFromNameCache.putIfAbsent(typeName, tpe)
      tpe
    }
  }

  private def outputSizeHint(tpe: Type): OutputSizeHint = {
    val cached = outputSizeHints.get(tpe)
    if (cached != null) {
      cached
    } else {
      val hint = new OutputSizeHint
      val previous = outputSizeHints.putIfAbsent(tpe, hint)
      if (previous == null) hint else previous
    }
  }

  /**
   * Creates object from gives json String, based on a declared type.
   */
  def deserialize[T](json: String)(implicit tag: TypeTag[T]): T = {
    val tpe = extractType(tag)
    if (isNothing(tpe)) {
      throw new IllegalArgumentException("Type for object deserialization was not specified, or was Nothing. Please specify object type.")
    } else {
      deserialize(json, tpe)
    }
  }

  /**
   * Creates object from gives json String, based on a passed type.
   */
  def deserialize[T](json: String, tpe: Type): T = {
    val jsonIterator = new StringIterator(json, config.strict)
    deserialize(jsonIterator, tpe)
  }

  /**
    * Creates object from gives json String, based on a passed type.
    */
  def deserialize[T](jsonIterator: StringIterator, tpe: Type): T = {
    try {
      val result = if (jsonIterator.tryConsumeNull()) {
        NullValues.forType(tpe, config).get().asInstanceOf[T]
      } else {
        deserializerFactory.getDeserializer(tpe, rootContext).deserialize(jsonIterator)
      }
      jsonIterator.expectEnd()
      result
    } catch {
      case e: RuntimeException =>
        throw new JsonInnerException(ErrorMessageFormatter.formatDeserializationError(jsonIterator.stringValue, jsonIterator, e, tpe), e)
      case e: StackOverflowError =>
        throw ErrorMessageFormatter.tooDeep(jsonIterator, e, tpe)
    }
  }

  /**
   * Creates object from gives json String, based on a passed type name (class name).
   */
  def deserialize[T](json: String, typeName: String): T = {
    deserialize(json, extractTypeFromName(typeName))
  }


  /**
   * Creates a specialized deserializer for a declared type.
   */
  def buildStaticDeserializer[T](implicit tag: TypeTag[T]): StaticDeserializer[T] = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for object deserialization was not specified, or was Nothing. Please specify object type.")
    } else {
      buildStaticDeserializer(typeTag[T].tpe)
    }
  }

  /**
   * Creates a specialized deserializer for a passed type.
   */
  def buildStaticDeserializer[T](tpe: Type): StaticDeserializer[T] = {
    new StaticDeserializer[T](deserializerFactory.getDeserializer[T](tpe, Context(List.empty, Map.empty)), tpe, config)
  }

  /**
   * Creates a specialized deserializer for a passed type name (class name).
   */
  def buildStaticDeserializer[T](typeName: String): StaticDeserializer[T] = {
    buildStaticDeserializer(extractTypeFromName(typeName))
  }

  /**
   * Creates a String containing JSON representation of given object.
   * @param obj object to serialize
   * @return JSON representation of given object
   */
  def serialize[T](obj: T)(implicit tag: TypeTag[T]): String = {
    val tpe = extractType(tag)
    if (isNothing(tpe)) {
      throw new IllegalArgumentException("Type for object serialization was not specified, or was Nothing. Please specify object type.")
    } else {
      serialize(obj, tpe)
    }
  }

  /**
   * Creates a String containing JSON representation of given object based on the given type
   * @param obj object to serialize
   * @param tpe type of object to serialize
   * @return JSON representation of given object
   */
  def serialize[T](obj: T, tpe: Type): String = {
    if (obj == null) {
      if (config.nullPolicy == NullPolicy.Fail) {
        throw new IllegalArgumentException("Null value is not allowed as the top-level value (NullPolicy.Fail); use NullPolicy.Omit or WriteNull to allow nulls")
      }
      "null"
    } else {
      val hint = outputSizeHint(tpe)
      val json = hint.newBuilder()
      serializerFactory.getSerializer(tpe, rootContext).serialize(obj, json)
      hint.update(json)
      json.toString()
    }
  }

  /**
   * Creates a String containing JSON representation of given object based on the given type name (class name)
   * @param obj object to serialize
   * @param typeName type name (class name) of object to serialize
   * @return JSON representation of given object
   */
  def serialize[T](obj: T, typeName: String): String = {
    serialize(obj, extractTypeFromName(typeName))
  }

  /**
   * Creates JSON representation of given object encoded in UTF-8: the same bytes as
   * `serialize(obj).getBytes(UTF_8)`, written without building the String first.
   * @param obj object to serialize
   * @return JSON representation of given object as UTF-8 bytes
   */
  def serializeToBytes[T](obj: T)(implicit tag: TypeTag[T]): Array[Byte] = {
    val tpe = extractType(tag)
    if (isNothing(tpe)) {
      throw new IllegalArgumentException("Type for object serialization was not specified, or was Nothing. Please specify object type.")
    } else {
      serializeToBytes(obj, tpe)
    }
  }

  /**
   * Creates JSON representation of given object based on the given type, encoded in UTF-8.
   * @param obj object to serialize
   * @param tpe type of object to serialize
   * @return JSON representation of given object as UTF-8 bytes
   */
  def serializeToBytes[T](obj: T, tpe: Type): Array[Byte] = {
    if (obj == null) {
      serialize(obj, tpe).getBytes(StandardCharsets.UTF_8)
    } else {
      val hint = outputSizeHint(tpe)
      val out = hint.newUtf8Output()
      Utf8Writer.of(serializerFactory.getSerializer[T](tpe, rootContext)).writeUtf8(obj, out)
      hint.update(out)
      out.toByteArray
    }
  }

  /**
   * Creates JSON representation of given object based on the given type name (class name), encoded in UTF-8.
   * @param obj object to serialize
   * @param typeName type name (class name) of object to serialize
   * @return JSON representation of given object as UTF-8 bytes
   */
  def serializeToBytes[T](obj: T, typeName: String): Array[Byte] = {
    serializeToBytes(obj, extractTypeFromName(typeName))
  }

  /**
   * Creates a specialized serializer for a passed type name (class name).
   */
  def buildStaticSerializer[T](implicit tag: TypeTag[T]): StaticSerializer[T] = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for object serialization was not specified, or was Nothing. Please specify object type.")
    } else {
      buildStaticSerializer(tag.tpe)
    }
  }

  /**
   * Creates a specialized serializer for a passed type name (class name).
   */
  def buildStaticSerializer[T](tpe: Type): StaticSerializer[T] = {
    new StaticSerializer[T](serializerFactory.getSerializer[T](tpe, Context(List.empty, Map.empty)), config)
  }

  /**
   * Creates a specialized serializer for a passed type name (class name).
   */
  def buildStaticSerializer[T](typeName: String): StaticSerializer[T] = {
    buildStaticSerializer(extractTypeFromName(typeName))
  }


  /**
   * Method to register custom json converter to support custom types of data.
   * @param serializer serializer that will be used to serialize given type
   * @param deserializer deserializer that will be used to serialize given type
   */
  def registerConverter[T: TypeTag](serializer: SerializerFactory => JsonTypeSerializer[T],
                                    deserializer: DeserializerFactory => JsonTypeDeserializer[T])(implicit tag: TypeTag[T]): Unit = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for converter was not specified, or was Nothing. Please specify object type.")
    } else {
      serializerFactory.registerSerializer[T](extractType(tag), serializer)
      deserializerFactory.registerDeserializer[T](extractType(tag), deserializer)
    }
  }

  /**
   * Method to register custom json converter to support custom types of data _and_all_types_that_are_extending_it_.
   * @param serializer serializer that will be used to serialize and deserialize given type and its descendant
   * @param deserializer deserializer that will be used to serialize and deserialize given type and its descendant
   */
  def registerSuperclassConverter[T](serializer: SerializerFactory => JsonTypeSerializer[T],
                                     deserializer: DeserializerFactory => JsonTypeDeserializer[T])(implicit tag: TypeTag[T]): Unit = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for converter was not specified, or was Nothing. Please specify object type.")
    } else {
      serializerFactory.registerSuperclassSerializer[T](extractType(tag), serializer)
      deserializerFactory.registerSuperclassDeserializer[T](extractType(tag), deserializer)
    }
  }

  /**
   * Method to register custom json converter to support custom types of data _and_all_types_that_are_extending_it_.
   * @param serializer serializer that will be used to serialize  given type and its descendant, it will handle any subtype of given type, eg. Something[T](value: T)
   */
  def registerSerializerForAnySubtype[T](serializer: SerializerFactory => JsonTypeSerializer[T])(implicit tag: TypeTag[T]): Unit = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for converter was not specified, or was Nothing. Please specify object type.")
    } else {
      serializerFactory.registerSerializerAllSubTypes[T](extractType(tag), serializer)
    }
  }


  /** Builds the typed deserializer for a marked type: subtypes resolve via the sealed hierarchy or as siblings of the marked type. */
  private def typedDeserializerFor[T <: AnyRef](tpe: Type, readTable: Map[String, Class[_]], deserializerFactory: DeserializerFactory): TypedDeserializer[T] = {
    val binaryName = TypesUtil.getClassFromType(tpe).getName
    val prefix = binaryName.substring(0, Math.max(binaryName.lastIndexOf('$'), binaryName.lastIndexOf('.')) + 1)
    new TypedDeserializer[T](prefix, readTable, tpe.toString, TypesUtil.getClassFromType(tpe), deserializerFactory)
  }

  /**
   * Method that allows to specify which type will have added type information to serialized json,
   * and which can be deserialized based on this specified type.
   */
  def markTypedClass[T <: AnyRef]()(implicit tag: TypeTag[T]): Unit = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for typed class was not specified, or was Nothing. Please specify object type.")
    } else {
      val tpe = extractType(tag)
      val readTable = TypeNames.readTable(tpe.typeSymbol)
      registerConverter[T](
        sf => new TypedSerializer[T](sf, readTable, tpe.toString),
        df => typedDeserializerFor[T](tpe, readTable, df))
    }
  }

  /**
    * Method that allows to specify which type will have added type information to serialized json,
    * and which can be deserialized based on this specified type.
    * This will make any subclass of given class typed.
    * Basically should not be used, it's here for backward compatibility.
    */
  def markTypedSuperClass[T <: AnyRef]()(implicit tag: TypeTag[T]): Unit = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for typed class was not specified, or was Nothing. Please specify object type.")
    } else {
      val tpe = extractType(tag)
      val readTable = TypeNames.readTable(tpe.typeSymbol)
      registerSuperclassConverter[T](
        sf => new TypedSerializer[T](sf, readTable, tpe.toString),
        df => typedDeserializerFor[T](tpe, readTable, df))
    }
  }


  /**
    * Method that allows object to be transformed after deserialization, e.g. to get rid of nulls for missing JSON properties.
    */
  def postDeserializationTransform[T](transform: T => T)(implicit tag: TypeTag[T]): Unit = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for typed class was not specified, or was Nothing. Please specify object type.")
    } else {
      val tpe = extractType(tag)
      deserializerFactory.registerPostTransform[T](extractType(tag), transform)
    }
  }

}
