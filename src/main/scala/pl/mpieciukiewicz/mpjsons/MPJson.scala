package pl.mpieciukiewicz.mpjsons

import pl.mpieciukiewicz.mpjsons.impl.util.ClassType
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator, JsonInnerException}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.BeanDeserializer
import pl.mpieciukiewicz.mpjsons.impl.serializer.BeanSerializer
import java.lang.reflect.Field
import scala.reflect.runtime.universe._

/**
 * Main class of json serialization library.
 * @author Marcin Pieciukiewicz
 */
object MPJson {

  /**
   * Creates object from gives json String and type of class.
   * @param json String containing json that represents object of given clazz
   * @param clazz type f the object to deserialize
   * @return created object
   */
  def deserialize[T](json: String)(implicit tag: TypeTag[T]): T = {
    deserializeWithField(json)
  }

  /**
   * Creates object from gives json String and type of class.
   * @param json String containing json that represents object of given clazz
   * @param clazz type f the object to deserialize
   * @return created object
   */
  def deserializeGeneric[T](json: String)(implicit tag: TypeTag[T]): T = {
    deserializeWithField(json)
  }

  /**
   * Creates object from gives json String and type of class.
   * @param json String containing json that represents object of given clazz
   * @param clazz type f the object to deserialize
   * @return created object
   */
  private def deserializeWithField[T](json: String)(implicit tag: TypeTag[T]): T = {
    val jsonIterator = new StringIterator(json)
    try {
      DeserializerFactory.getDeserializer(tag.tpe).deserialize(jsonIterator, ClassType(tag.tpe))
    } catch {
      case e: RuntimeException => throw new JsonInnerException("Problem deserializing:\n" + json + "\n" + jsonIterator.debugShowConsumedString, e)
    }
  }

  /**
   * Creates json String that represents given object.
   * @param obj object to serialize
   * @return json String
   */
  def serialize[T](obj: T)(implicit tag: TypeTag[T]): String = {
    val json = new StringBuilder()
    SerializerFactory.getSerializer(tag.tpe).serialize(obj, json)
    json.toString()
  }

  /**
   * Method to register custom json converter to support custom types of data.
   * @param clazz type that is supported by given converter
   * @param converter converter that will be used to serialize and deserialize given type
   */
  def registerConverter[T](converter: JsonTypeConverter[T])(implicit tag: TypeTag[T]) {
    SerializerFactory.registerSerializer[T](tag.tpe, converter)
    DeserializerFactory.registerDeserializer(tag.tpe, converter)
  }

  /**
   * Method to register custom json converter to support custom types of data _and_all_types_that_are_extending_it_.
   * @param clazz type that is supported by given converter
   * @param converter converter that will be used to serialize and deserialize given type and its descendant
   */
  def registerSuperclassConverter[T](converter: JsonTypeConverter[T])(implicit tag: TypeTag[T]) {
    SerializerFactory.registerSuperclassSerializer(tag.tpe, converter)
    DeserializerFactory.registerSuperclassDeserializer(tag.tpe, converter)
  }

}
