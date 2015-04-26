package io.mpjsons

import io.mpjsons.impl.SerializerFactory

/**
 * Class that supports serialization object to json String.
 * @author Marcin Pieciukiewicz
 */
abstract class JsonTypeSerializer[T] {
  /**
   * Creates json String that represents given object.
   * @param obj object to serialize
   * @return json String
   */
  def serialize(obj: T, jsonBuilder: StringBuilder): Unit
}
