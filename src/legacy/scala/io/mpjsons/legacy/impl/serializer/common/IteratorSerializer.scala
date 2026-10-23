package io.mpjsons.legacy.impl.serializer.common

import io.mpjsons.legacy.{JsonTypeSerializer, NullPolicy}
import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * Null elements are written as the `null` literal when the NullPolicy allows nulls, and rejected under Fail.
 * @author Marcin Pieciukiewicz
 */

abstract class IteratorSerializer[T, C](private val serializerFactory: SerializerFactory, private val tpe: Type, context: Context)
  extends JsonTypeSerializer[C] {

  protected val subTypeSerializer: JsonTypeSerializer[T] = serializerFactory.getSerializer(TypesUtil.getSubElementsType(tpe), context).asInstanceOf[JsonTypeSerializer[T]]
  private val nullsAllowed = serializerFactory.config.nullPolicy != NullPolicy.Fail

  protected def serializeIterator(iterator: Iterator[T], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('[')

    var isNotFirstField = false

    while (iterator.hasNext) {
      if (isNotFirstField) {
        jsonBuilder.append(',')
      } else {
        isNotFirstField = true
      }
      serializeElement(iterator.next(), jsonBuilder)
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
      serializeElement(remaining.head, jsonBuilder)
      remaining = remaining.tail
    }

    jsonBuilder.append(']')
  }

  private def serializeElement(element: T, jsonBuilder: StringBuilder): Unit = {
    if (element == null) {
      NullValues.writeNull(jsonBuilder, nullsAllowed, "an element of " + tpe)
    } else {
      subTypeSerializer.serialize(element, jsonBuilder)
    }
  }
}
