package pl.mpieciukiewicz.mpjsons

import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, JsonInnerException, SerializerFactory, StringIterator}

import scala.reflect.runtime.universe._

object MPJsons {
  implicit val deserializerFactory = new DeserializerFactory
  implicit val serializerFactory = new SerializerFactory
}

private object ErrorMessageFormatter {

  final val trimmingSize = 40

  def formatDeserializationError(json: String, jsonIterator: StringIterator, e: RuntimeException): String = {
    val consumed = jsonIterator.debugGetConsumedString
    val remaining = jsonIterator.debugGetRemainingString
    val consumedTrimmed = consumed.substring(Math.max(0, consumed.length - trimmingSize), consumed.length)
    val remainingTrimmed = remaining.substring(0, Math.min(remaining.length, trimmingSize))
    val consumedPlaceholder = " " * consumedTrimmed.length
    """
      |Problem deserializing: $json
      |Problem: ${e.getMessage}
      |In this place:
      |...$consumedTrimmed$remainingTrimmed...
      | $consumedPlaceholder^""".stripMargin
  }
}

class StaticSerializer[T](private val innerSerializer: JsonTypeSerializer[T], tpe: Type){
  def serialize(obj: T): String = {
    val json = new StringBuilder()
    innerSerializer.serialize(obj, json)
    json.toString()
  }
}

class StaticDeserializer[T](private val innerDeserializer: JsonTypeDeserializer[T], tpe: Type){
  def deserialize(json: String): T = {
    val jsonIterator = new StringIterator(json)
    try {
      innerDeserializer.deserialize(jsonIterator)
    } catch {
      case e: RuntimeException =>
        throw new JsonInnerException(ErrorMessageFormatter.formatDeserializationError(json, jsonIterator, e), e)
    }
  }

}

/**
 * Main class of json serialization library.
 * @author Marcin Pieciukiewicz
 */
class MPJsons {
import MPJsons._

  private var extractTypeCache = Map[TypeTag[_], Type]()
  private var extractTypeFromNameCache = Map[String, Type]()

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
   * Creates object from gives json String and type of class.
   * @param json String containing json that represents object of given clazz
   * @return created object
   */
  def deserialize[T](json: String)(implicit tag: TypeTag[T]): T = {
    deserialize(json, extractType(tag))
  }

  def deserialize[T](json: String, tpe: Type): T = {
    val jsonIterator = new StringIterator(json)
    try {
      deserializerFactory.getDeserializer(tpe).deserialize(jsonIterator)
    } catch {
      case e: RuntimeException =>
        throw new JsonInnerException(ErrorMessageFormatter.formatDeserializationError(json, jsonIterator, e), e)
    }
  }

  def deserialize[T](json: String, typeName: String): T = {
    deserialize(json, extractTypeFromName(typeName))
  }

  def getStaticDeserializer[T](implicit tag: TypeTag[T]): StaticDeserializer[T] = {
    getStaticDeserializer(tag.tpe)
  }

  def getStaticDeserializer[T](tpe: Type): StaticDeserializer[T] = {
    new StaticDeserializer[T](deserializerFactory.getDeserializer[T](tpe), tpe)
  }

  def getStaticDeserializer[T](typeName: String): StaticDeserializer[T] = {
    getStaticDeserializer(extractTypeFromName(typeName))
  }

  /**
   * Creates json String that represents given object.
   * @param obj object to serialize
   * @return json String
   */
  def serialize[T](obj: T)(implicit tag: TypeTag[T]): String = {
   serialize(obj, extractType(tag))
  }
  
  def serialize[T](obj: T, tpe: Type): String = {
    val json = new StringBuilder()
    serializerFactory.getSerializer(tpe).serialize(obj, json)
    json.toString()
  }

  def serialize[T](obj: T, typeName: String): String = {
    serialize(obj, extractTypeFromName(typeName))
  }

  def getStaticSerializer[T](implicit tag: TypeTag[T]): StaticSerializer[T] = {
    getStaticSerializer(tag.tpe)
  }

  def getStaticSerializer[T](tpe: Type): StaticSerializer[T] = {
    new StaticSerializer[T](serializerFactory.getSerializer[T](tpe), tpe)
  }

  def getStaticSerializer[T](typeName: String): StaticSerializer[T] = {
    getStaticSerializer(extractTypeFromName(typeName))
  }


  /**
   * Method to register custom json converter to support custom types of data.
   * @param converter converter that will be used to serialize and deserialize given type
   */
  def registerConverter[T](converter: JsonTypeConverter[T])(implicit tag: TypeTag[T]) {
    serializerFactory.registerSerializer[T](extractType(tag), converter)
    deserializerFactory.registerDeserializer(extractType(tag), converter)
  }

  /**
   * Method to register custom json converter to support custom types of data _and_all_types_that_are_extending_it_.
   * @param converter converter that will be used to serialize and deserialize given type and its descendant
   */
  def registerSuperclassConverter[T](converter: JsonTypeConverter[T])(implicit tag: TypeTag[T]) {
    serializerFactory.registerSuperclassSerializer(extractType(tag), converter)
    deserializerFactory.registerSuperclassDeserializer(extractType(tag), converter)
  }

}
