package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonMapDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */
class MapDeserializer[K, V](private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
extends AbstractJsonMapDeserializer[K, V, Map[K, V]](deserializerFactory, tpe, context) {

override def deserialize(jsonIterator: StringIterator): Map[K, V] = readBuffer(jsonIterator).toMap
}