package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object ProductSerializer extends JsonTypeSerializer {

  def serialize(obj: Any, jsonBuilder: StringBuilder) = {

    val tuple = obj.asInstanceOf[Product]

    jsonBuilder.append('[')

    var isNotFirstField = false

    val iterator = tuple.productIterator

    iterator.foreach(element => {
      if (isNotFirstField) {
        jsonBuilder.append(",")
      } else {
        isNotFirstField = true
      }
      SerializerFactory.getSerializer(element.getClass).serialize(element, jsonBuilder)
    })


    jsonBuilder.append(']')
  }
}