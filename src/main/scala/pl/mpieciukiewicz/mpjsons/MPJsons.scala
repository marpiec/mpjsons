package pl.mpieciukiewicz.mpjsons

import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, JsonInnerException, SerializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * Main class of json serialization library.
 * @author Marcin Pieciukiewicz
 */
class MPJsons {

  implicit val deserializerFactory = new DeserializerFactory
  implicit val serializerFactory = new SerializerFactory

  /**
   * Creates object from gives json String and type of class.
   * @param json String containing json that represents object of given clazz
   * @return created object
   */
  def deserialize[T](json: String)(implicit tag: TypeTag[T]): T = {
    deserialize(json, tag.tpe)
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
   serialize(obj, tag.tpe)
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
    serializerFactory.registerSerializer[T](tag.tpe, converter)
    deserializerFactory.registerDeserializer(tag.tpe, converter)
  }

  /**
   * Method to register custom json converter to support custom types of data _and_all_types_that_are_extending_it_.
   * @param converter converter that will be used to serialize and deserialize given type and its descendant
   */
  def registerSuperclassConverter[T](converter: JsonTypeConverter[T])(implicit tag: TypeTag[T]) {
    serializerFactory.registerSuperclassSerializer(tag.tpe, converter)
    deserializerFactory.registerSuperclassDeserializer(tag.tpe, converter)
  }

}
