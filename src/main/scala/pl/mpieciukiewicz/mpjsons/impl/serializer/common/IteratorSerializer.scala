package pl.mpieciukiewicz.mpjsons.impl.serializer.common

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

abstract class IteratorSerializer[T, C](private val serializerFactory: SerializerFactory, private val tpe: Type)
  extends JsonTypeSerializer[C] {

  val subTypeSerializer = serializerFactory.getSerializer(TypesUtil.getSubElementsType(tpe)).asInstanceOf[JsonTypeSerializer[T]]

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
