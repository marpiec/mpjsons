package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonMapDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.{Map, HashMap}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
class HashMapDeserializer[K, V](private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Map[Symbol, Type])
  extends AbstractJsonMapDeserializer[K, V, HashMap[K, V]](deserializerFactory, tpe, context) {

  /**
   * Creates object from gives json String and type of class.
   * @param jsonIterator StringIterator containing json that represents object of given clazz
   * @return created object
   */
  override def deserialize(jsonIterator: StringIterator): HashMap[K, V] = {
    val buffer = readBuffer(jsonIterator)
    HashMap(buffer.toArray: _*)
  }

}
