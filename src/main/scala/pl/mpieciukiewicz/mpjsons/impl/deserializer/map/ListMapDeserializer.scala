package pl.mpieciukiewicz.mpjsons.impl.deserializer.map

import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.ListMap
import scala.reflect.runtime.universe._


class ListMapDeserializer[K, V](private val deserializerFactory: DeserializerFactory, private val tpe: Type)
  extends AbstractJsonMapDeserializer[K, V, ListMap[K, V]](deserializerFactory, tpe) {


  override def deserialize(jsonIterator: StringIterator): ListMap[K, V] = {
    val buffer = readBuffer(jsonIterator)
    ListMap(buffer.toArray: _*)
  }

}