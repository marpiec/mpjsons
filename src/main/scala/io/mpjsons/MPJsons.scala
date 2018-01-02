package io.mpjsons

import io.mpjsons.impl.special.TypedConverter.{TypedDeserializer, TypedSerializer}
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, JsonInnerException, SerializerFactory, StringIterator}

import scala.reflect.runtime.universe._


private object ErrorMessageFormatter {

  final val trimmingSize = 40

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
 * Serializer for a single type.
 */
class StaticSerializer[T](private val innerSerializer: JsonTypeSerializer[T], tpe: Type) {

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
    val json = new StringBuilder()
    innerSerializer.serialize(obj, json)
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
 * Main class of json serialization library.
 * @author Marcin Pieciukiewicz
 */
class MPJsons {

  private val deserializerFactory = new DeserializerFactory
  private val serializerFactory = new SerializerFactory

  private var extractTypeCache = Map[TypeTag[_], Type]()
  private var extractTypeFromNameCache = Map[String, Type]()

  private val nothingTypeTag = typeTag[Nothing]

  private def extractType[T](tag: TypeTag[T]): Type = {
    extractTypeCache.getOrElse(tag, {
      val tpe = tag.tpe
      extractTypeCache += tag -> tpe
      tpe
    })
  }

  private def extractTypeFromName[T](typeName: String): Type = {
    extractTypeFromNameCache.getOrElse(typeName, {
      val tpe = TypesUtil.getTypeFromClass(Class.forName(typeName))
      extractTypeFromNameCache += typeName -> tpe
      tpe
    })
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
      deserializerFactory.getDeserializer(tpe, Context(List(), Map())).deserialize(jsonIterator)
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
    new StaticDeserializer[T](deserializerFactory.getDeserializer[T](tpe, Context(List(), Map())), tpe)
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
    val json = new StringBuilder()
    serializerFactory.getSerializer(tpe, Context(List(), Map())).serialize(obj, json)
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
    new StaticSerializer[T](serializerFactory.getSerializer[T](tpe, Context(List(), Map())), tpe)
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
                                    deserializer: DeserializerFactory => JsonTypeDeserializer[T])(implicit tag: TypeTag[T]) {
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
   * Method that allows to specify which type will have added type information to serialized json,
   * and which can be deserialized based on this specified type.
   */
  def markTypedClass[T <: AnyRef]()(implicit tag: TypeTag[T]): Unit = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for typed class was not specified, or was Nothing. Please specify object type.")
    } else {
      val tpe = extractType(tag)
      val typeName: String = tpe.baseClasses.head.fullName
      val packageName = typeName.substring(0, typeName.lastIndexOf('.'))
      registerConverter[T](
        sf => new TypedSerializer[T](packageName, sf),
        df => new TypedDeserializer[T](packageName, df))
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
      val typeName: String = tpe.baseClasses.head.fullName
      val packageName = typeName.substring(0, typeName.lastIndexOf('.'))
      registerSuperclassConverter[T](
        sf => new TypedSerializer[T](packageName, sf),
        df => new TypedDeserializer[T](packageName, df))
    }
  }


  /**
    * Method that allows object to be transformed after deserialization, e.g. to get rid of nulls for missing JSON properties.
    */
  def postDeserializationTransform[T](transform: (T) => T)(implicit tag: TypeTag[T]): Unit = {
    if (tag == nothingTypeTag) {
      throw new IllegalArgumentException("Type for typed class was not specified, or was Nothing. Please specify object type.")
    } else {
      val tpe = extractType(tag)
      deserializerFactory.registerPostTransform[T](extractType(tag), transform)
    }
  }

}
