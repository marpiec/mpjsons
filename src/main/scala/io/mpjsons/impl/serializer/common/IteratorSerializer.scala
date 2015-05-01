package io.mpjsons.impl.serializer.common

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.TypesUtil

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */

abstract class IteratorSerializer[T, C](private val serializerFactory: SerializerFactory, private val tpe: Type, context: Context)
  extends JsonTypeSerializer[C] {

  val subTypeSerializer = serializerFactory.getSerializer(TypesUtil.getSubElementsType(tpe), context).asInstanceOf[JsonTypeSerializer[T]]

  protected def serializeIterator(iterator: Iterator[T], jsonBuilder: StringBuilder) {

    jsonBuilder.append('[')

    var isNotFirstField = false

    iterator.foreach(element => {

      if (isNotFirstField) {
        jsonBuilder.append(',')
      } else {
        isNotFirstField = true
      }
      subTypeSerializer.serialize(element, jsonBuilder)
    })


    jsonBuilder.append(']')
  }
}
