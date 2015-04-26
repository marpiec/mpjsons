package io.mpjsons

import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * Class that supports deserialization of json String to object.
 * @author Marcin Pieciukiewicz
 */
trait JsonTypeDeserializer[T] {
  /**
   * Creates object from gives json String and type of class.
   * @param jsonIterator StringIterator containing json that represents object of given clazz
   * @return created object
   */
  def deserialize(jsonIterator: StringIterator): T
}