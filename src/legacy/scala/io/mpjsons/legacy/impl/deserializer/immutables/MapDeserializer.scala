package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonMapDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
class MapDeserializer[K, V](private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
  extends AbstractJsonMapDeserializer[K, V, Map[K, V]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): Map[K, V] = readInto(jsonIterator, Map.newBuilder[K, V])
}