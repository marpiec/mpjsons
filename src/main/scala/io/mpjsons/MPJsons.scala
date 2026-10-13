package io.mpjsons

import io.mpjsons.impl.special.TypedConverter.{TypedDeserializer, TypedSerializer}
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, JsonInnerException, SerializerFactory, StringIterator}

import java.util.concurrent.ConcurrentHashMap
import scala.annotation.StaticAnnotation
import scala.reflect.runtime.universe._


private object ErrorMessageFormatter {

  private final val trimmingSize = 40

  def formatDeserializationError(json: String, jsonIterator: StringIterator, e: RuntimeException, tpe: Type): String = {
    val consumed = jsonIterator.debugGetConsumedString
    val remaining = jsonIterator.debugGetRemainingString
    val consumedTrimmed = consumed.substring(Math.max(0, consumed.length - trimmingSize), consumed.length)
    val remainingTrimmed = remaining.substring(0, Math.min(remaining.length, trimmingSize))
    val consumedPlaceholder = " " * consumedTrimmed.length
    s"""|Problem deserializing: $tpe
        |Problem: ${e.toString}
        |In this place:
        |...$consumedTrimmed$remainingTrimmed...
        | $consumedPlaceholder^
        |Json: $json""".stripMargin
  }
}

/**
 * Remembers the length of the last output produced for one type, so the next output buffer can be
 * allocated at the right size instead of growing from the default capacity in several steps.
 * Races between threads only make the hint slightly stale, which is harmless.
 */
private[mpjsons] final class OutputSizeHint {
  @volatile private var length: Int = 64

  def newBuilder(): StringBuilder = new StringBuilder(length + 16)

  def update(builder: StringBuilder): Unit = {
    length = builder.length
  }
}

/**
 * Serializer for a single type.
 */
class StaticSerializer[T](private val innerSerializer: JsonTypeSerializer[T]) {

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
    innerSerializer.serialize(obj, json)
    sizeHint.update(json)
    json
  }
}

/**
 * Deserializer for a single type.
 */
class StaticDeserializer[T](private val innerDeserializer: JsonTypeDeserializer[T], tpe: Type) {

  /**
   * Takes a String containing JSON representation of objects and returns an instance of given object.
   */
  def deserialize(json: String): T = {
    val jsonIterator = new StringIterator(json)
    try {
      innerDeserializer.deserialize(jsonIterator)
    } catch {
      case e: RuntimeException =>
        throw new JsonInnerException(ErrorMessageFormatter.formatDeserializationError(json, jsonIterator, e, tpe), e)
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
class MPJsons(ignoreNonExistingFields: Boolean = false, ignoreNullFields: Boolean = false) {

  private val deserializerFactory = new DeserializerFactory(ignoreNonExistingFields)
  private val serializerFactory = new SerializerFactory(ignoreNullFields)

  private val extractTypeFromNameCache = new ConcurrentHashMap[String, Type]()
  private val outputSizeHints = new ConcurrentHashMap[Type, OutputSizeHint]()

  private val nothingTypeTag = typeTag[Nothing]

  // A materialised TypeTag is a fresh object on every call and its equals/hashCode evaluate tag.tpe anyway,
  // so caching by TypeTag cannot save that computation; tag.tpe itself is cheap for interned types.
  private def extractType[T](tag: TypeTag[T]): Type = tag.tpe

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
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for object deserialization was not specified, or was Nothing. Please specify object type.")
    } else {
      deserialize(json, extractType(tag))
    }
  }

  /**
   * Creates object from gives json String, based on a passed type.
   */
  def deserialize[T](json: String, tpe: Type): T = {
    val jsonIterator = new StringIterator(json)
    deserialize(jsonIterator, tpe)
  }

  /**
    * Creates object from gives json String, based on a passed type.
    */
  def deserialize[T](jsonIterator: StringIterator, tpe: Type): T = {
    try {
      deserializerFactory.getDeserializer(tpe, Context(List.empty, Map.empty)).deserialize(jsonIterator)
    } catch {
      case e: RuntimeException =>
        throw new JsonInnerException(ErrorMessageFormatter.formatDeserializationError(jsonIterator.stringValue, jsonIterator, e, tpe), e)
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
    new StaticDeserializer[T](deserializerFactory.getDeserializer[T](tpe, Context(List.empty, Map.empty)), tpe)
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
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for object serialization was not specified, or was Nothing. Please specify object type.")
    } else {
      serialize(obj, extractType(tag))
    }
  }

  /**
   * Creates a String containing JSON representation of given object based on the given type
   * @param obj object to serialize
   * @param tpe type of object to serialize
   * @return JSON representation of given object
   */
  def serialize[T](obj: T, tpe: Type): String = {
    val hint = outputSizeHint(tpe)
    val json = hint.newBuilder()
    serializerFactory.getSerializer(tpe, Context(List.empty, Map.empty)).serialize(obj, json)
    hint.update(json)
    json.toString()
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
    new StaticSerializer[T](serializerFactory.getSerializer[T](tpe, Context(List.empty, Map.empty)))
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
  private def typedDeserializerFor[T <: AnyRef](tpe: Type, deserializerFactory: DeserializerFactory): TypedDeserializer[T] = {
    val binaryName = TypesUtil.getClassFromType(tpe).getName
    val prefix = binaryName.substring(0, Math.max(binaryName.lastIndexOf('$'), binaryName.lastIndexOf('.')) + 1)
    new TypedDeserializer[T](prefix, knownSubclasses(tpe.typeSymbol), tpe.toString, deserializerFactory)
  }

  /** Simple class name -> class for every (transitive) known subclass of a sealed type; empty otherwise. */
  private def knownSubclasses(symbol: Symbol): Map[String, Class[_]] = {
    if (symbol.isClass && symbol.asClass.isSealed) {
      symbol.asClass.knownDirectSubclasses.toList.flatMap { sub =>
        val clazz = TypesUtil.getClassFromType(sub.asClass.toType)
        (clazz.getSimpleName -> clazz) :: knownSubclasses(sub).toList
      }.toMap
    } else {
      Map.empty
    }
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
      registerConverter[T](
        sf => new TypedSerializer[T](sf),
        df => typedDeserializerFor[T](tpe, df))
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
      registerSuperclassConverter[T](
        sf => new TypedSerializer[T](sf),
        df => typedDeserializerFor[T](tpe, df))
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
