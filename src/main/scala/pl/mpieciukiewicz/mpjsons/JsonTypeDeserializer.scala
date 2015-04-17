package pl.mpieciukiewicz.mpjsons

import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * Class that supports deserialization of json String to object.
 * @author Marcin Pieciukiewicz
 */
trait JsonTypeDeserializer[T] {
  /**
   * Creates object from gives json String and type of class.
   * @param jsonIterator StringIterator containing json that represents object of given clazz
   * @param tpe type f the object to deserialize
   * @return created object
   */
  def deserialize(jsonIterator: StringIterator, tpe: Type)(implicit deserializerFactory: DeserializerFactory): T
}