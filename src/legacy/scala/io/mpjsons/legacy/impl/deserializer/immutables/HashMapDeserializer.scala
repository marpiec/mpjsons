package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonMapDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.{ArraySeq, HashMap}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
class HashMapDeserializer[K, V](private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
  extends AbstractJsonMapDeserializer[K, V, HashMap[K, V]](deserializerFactory, tpe, context) {

  /**
   * Creates object from gives json String and type of class.
   * @param jsonIterator StringIterator containing json that represents object of given clazz
   * @return created object
   */
  override def deserialize(jsonIterator: StringIterator): HashMap[K, V] = {
    readInto(jsonIterator, HashMap.newBuilder[K, V])
  }

}
