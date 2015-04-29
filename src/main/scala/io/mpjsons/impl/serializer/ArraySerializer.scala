package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ArraySerializer[E](serializerFactory: SerializerFactory, tpe: Type, context: Map[Symbol, Type])
  extends IteratorSerializer[E, Array[E]](serializerFactory, tpe, context) {

  override def serialize(obj: Array[E], jsonBuilder: StringBuilder) {
    serializeIterator(obj.iterator, jsonBuilder)
  }

}













