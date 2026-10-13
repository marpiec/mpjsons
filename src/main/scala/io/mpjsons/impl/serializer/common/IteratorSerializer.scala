package io.mpjsons.impl.serializer.common

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

abstract class IteratorSerializer[T, C](private val serializerFactory: SerializerFactory, private val tpe: Type, context: Context)
  extends JsonTypeSerializer[C] {

  protected val subTypeSerializer: JsonTypeSerializer[T] = serializerFactory.getSerializer(TypesUtil.getSubElementsType(tpe), context).asInstanceOf[JsonTypeSerializer[T]]

  protected def serializeIterator(iterator: Iterator[T], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('[')

    var isNotFirstField = false

    while (iterator.hasNext) {
      if (isNotFirstField) {
        jsonBuilder.append(',')
      } else {
        isNotFirstField = true
      }
      subTypeSerializer.serialize(iterator.next(), jsonBuilder)
    }

    jsonBuilder.append(']')
  }

  /** Walks a List without allocating an iterator. */
  protected def serializeList(list: List[T], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('[')

    var remaining = list
    var isNotFirstField = false

    while (remaining ne Nil) {
      if (isNotFirstField) {
        jsonBuilder.append(',')
      } else {
        isNotFirstField = true
      }
      subTypeSerializer.serialize(remaining.head, jsonBuilder)
      remaining = remaining.tail
    }

    jsonBuilder.append(']')
  }
}
