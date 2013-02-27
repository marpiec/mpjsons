package pl.marpiec.mpjsons

import java.lang.reflect.Field
/**
 * Class that supports deserialization of json String to object.
 * @author Marcin Pieciukiewicz
 */
trait JsonTypeDeserializer[T] {
  /**
   * Creates object from gives json String and type of class.
   * @param jsonIterator StringIterator containing json that represents object of given clazz
   * @param clazz type f the object to deserialize
   * @param field field object for a given clazz (if required) that gives the
   *              possibility to read FirstSubType and SecondSubType annotations. Might be null.
   * @return created object
   */
  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): T
}