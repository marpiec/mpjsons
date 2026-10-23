package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.serializer.common.IteratorSerializer
import io.mpjsons.legacy.impl.util.Context

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class IterableSerializer[E](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends IteratorSerializer[E, Iterable[E]](serializerFactory, tpe, context) {

  override def serialize(obj: Iterable[E], jsonBuilder: StringBuilder): Unit = {
    obj match {
      case list: List[E] => serializeList(list, jsonBuilder)
      case other => serializeIterator(other.iterator, jsonBuilder)
    }
  }

}
