package io.mpjsons.impl.serializer

import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import io.mpjsons.impl.util.Context

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class IterableSerializer[E](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends IteratorSerializer[E, Iterable[E]](serializerFactory, tpe, context) {

  override def serialize(obj: Iterable[E], jsonBuilder: StringBuilder) {
    serializeIterator(obj.iterator, jsonBuilder)
  }

}







