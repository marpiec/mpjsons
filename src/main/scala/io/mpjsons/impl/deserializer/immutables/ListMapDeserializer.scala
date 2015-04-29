package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonMapDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.{Map, ListMap}
import scala.reflect.runtime.universe._


class ListMapDeserializer[K, V](private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Map[Symbol, Type])
  extends AbstractJsonMapDeserializer[K, V, ListMap[K, V]](deserializerFactory, tpe, context) {


  override def deserialize(jsonIterator: StringIterator): ListMap[K, V] = {
    val buffer = readBuffer(jsonIterator)
    ListMap(buffer.toArray: _*)
  }

}