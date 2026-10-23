package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonMapDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.ListMap
import scala.reflect.runtime.universe._

class ListMapDeserializer[K, V](private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
  extends AbstractJsonMapDeserializer[K, V, ListMap[K, V]](deserializerFactory, tpe, context) {


  override def deserialize(jsonIterator: StringIterator): ListMap[K, V] = {
    readInto(jsonIterator, ListMap.newBuilder[K, V])
  }

}