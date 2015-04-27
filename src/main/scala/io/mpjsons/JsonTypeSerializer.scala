package io.mpjsons

/**
 * Class that supports serialization object to json String.
 * @author Marcin Pieciukiewicz
 */
trait JsonTypeSerializer[T] {
  /**
   * Creates json String that represents given object.
   * @param obj object to serialize
   * @return json String
   */
  def serialize(obj: T, jsonBuilder: StringBuilder): Unit
}
