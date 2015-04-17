package pl.mpieciukiewicz.mpjsons

import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, JsonInnerException, SerializerFactory, StringIterator}

import scala.reflect.runtime.universe._

object MPJsons {
  implicit val deserializerFactory = new DeserializerFactory
  implicit val serializerFactory = new SerializerFactory
}

/**
 * Main class of json serialization library.
 * @author Marcin Pieciukiewicz
 */
class MPJsons {
import MPJsons._


  private var extractTypeCache = Map[TypeTag[_], Type]()

  private def extractType[T](tag: TypeTag[T]): Type = {
    extractTypeCache.getOrElse(tag, {
      val tpe = tag.tpe
      extractTypeCache += tag -> tpe
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
      deserializerFactory.getDeserializer(tpe).deserialize(jsonIterator, tpe)
    } catch {
      case e: RuntimeException =>
        throw new JsonInnerException(
          s"Problem deserializing: $json ${jsonIterator.debugShowConsumedString}", e)
    }
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
