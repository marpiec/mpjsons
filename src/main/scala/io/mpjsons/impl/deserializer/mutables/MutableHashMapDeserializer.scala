package io.mpjsons.impl.deserializer.mutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonMapDeserializer
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable.HashMap
import scala.reflect.runtime.universe._

/**
 * Deserializer for mutable.HashMap and mutable.Map.
 */
class MutableHashMapDeserializer[K, V](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonMapDeserializer[K, V, HashMap[K, V]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): HashMap[K, V] = {
    readInto(jsonIterator, HashMap.newBuilder[K, V])
  }

}
