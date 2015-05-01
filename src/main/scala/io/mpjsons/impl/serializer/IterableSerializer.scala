package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */

class IterableSerializer[E](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends IteratorSerializer[E, Iterable[E]](serializerFactory, tpe, context) {

  override def serialize(obj: Iterable[E], jsonBuilder: StringBuilder) {
    serializeIterator(obj.iterator, jsonBuilder)
  }

}







