package io.mpjsons.impl.serializer

import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import io.mpjsons.impl.util.Context

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ArraySerializer[E](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends IteratorSerializer[E, Array[E]](serializerFactory, tpe, context) {

  override def serialize(obj: Array[E], jsonBuilder: StringBuilder): Unit = {
    serializeIterator(obj.iterator, jsonBuilder)
  }

}













