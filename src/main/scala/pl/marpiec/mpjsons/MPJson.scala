package pl.marpiec.mpjsons

import pl.marpiec.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator, JsonInnerException}
import pl.marpiec.mpjsons.impl.deserializer.BeanDeserializer
import pl.marpiec.mpjsons.impl.serializer.BeanSerializer
import java.lang.reflect.Field


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
  def deserialize[T](json: String, clazz: Class[T]): Any = {
    deserializeWithField(json, clazz, null)
  }

  /**
   * Creates object from gives json String and type of class.
   * @param json String containing json that represents object of given clazz
   * @param clazz type f the object to deserialize
   * @return created object
   */
  def deserializeGeneric[T](json: String, clazz: Class[T], field: Field): Any = {
    deserializeWithField(json, clazz, field)
  }

  /**
   * Creates object from gives json String and type of class.
   * @param json String containing json that represents object of given clazz
   * @param clazz type f the object to deserialize
   * @return created object
   */
  private def deserializeWithField[T](json: String, clazz: Class[T], field: Field): Any = {
    case class Tmp(f: T)
    val jsonIterator = new StringIterator(json)
    try {
      DeserializerFactory.getDeserializer(clazz).deserialize(jsonIterator, clazz, field)
    } catch {
      case e: RuntimeException => throw new JsonInnerException("Problem deserializing:\n" + json + "\n" + jsonIterator.debugShowConsumedString, e)
    }
  }

  /**
   * Creates json String that represents given object.
   * @param obj object to serialize
   * @return json String
   */
  def serialize(obj: AnyRef): String = {
    val json = new StringBuilder()
    SerializerFactory.getSerializer(obj.getClass).serialize(obj, json)
    json.toString()
  }

  /**
   * Method to register custom json converter to support custom types of data.
   * @param clazz type that is supported by given converter
   * @param converter converter that will be used to serialize and deserialize given type
   */
  def registerConverter(clazz: Class[_], converter: JsonTypeConverter[_]) {
    SerializerFactory.registerSerializer(clazz, converter)
    DeserializerFactory.registerDeserializer(clazz, converter)
  }

  /**
   * Method to register custom json converter to support custom types of data _and_all_types_that_are_extending_it_.
   * @param clazz type that is supported by given converter
   * @param converter converter that will be used to serialize and deserialize given type and its descendant
   */
  def registerSuperclassConverter(clazz: Class[_], converter: JsonTypeConverter[_]) {
    SerializerFactory.registerSuperclassSerializer(clazz, converter)
    DeserializerFactory.registerSuperclassDeserializer(clazz, converter)
  }

}
