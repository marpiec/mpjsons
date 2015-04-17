package pl.mpieciukiewicz.mpjsons.impl.serializer.common

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

/**
 * @author Marcin Pieciukiewicz
 */

abstract class IteratorSerializer[T] extends JsonTypeSerializer[T] {

  protected val serializerFactory: SerializerFactory

  protected def serializeIterator(iterator: Iterator[_], jsonBuilder: StringBuilder) {

    jsonBuilder.append('[')

    var isNotFirstField = false

    iterator.foreach(element => {

      if (isNotFirstField) {
        jsonBuilder.append(',')
      } else {
        isNotFirstField = true
      }
      serializerFactory.getSerializer(TypesUtil.getTypeFromClass(element.getClass))
        .asInstanceOf[JsonTypeSerializer[Any]].serialize(element, jsonBuilder)
    })


    jsonBuilder.append(']')
  }
}
