package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

class IterableSerializer[E](serializerFactory: SerializerFactory, tpe: Type)
  extends IteratorSerializer[E, Iterable[E]](serializerFactory, tpe) {

  override def serialize(obj: Iterable[E], jsonBuilder: StringBuilder) {
    serializeIterator(obj.iterator, jsonBuilder)
  }

}







