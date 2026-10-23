package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.serializer.common.IteratorSerializer
import io.mpjsons.legacy.impl.util.Context

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













