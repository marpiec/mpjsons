package pl.mpieciukiewicz.mpjsons.impl.deserializer.immutable

import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractJsonMapDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
class MapDeserializer[K, V](private val deserializerFactory: DeserializerFactory, private val tpe: Type)
extends AbstractJsonMapDeserializer[K, V, Map[K, V]](deserializerFactory, tpe) {

override def deserialize(jsonIterator: StringIterator): Map[K, V] = readBuffer(jsonIterator).toMap
}