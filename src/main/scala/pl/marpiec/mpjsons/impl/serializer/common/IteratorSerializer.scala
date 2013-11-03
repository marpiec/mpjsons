package pl.marpiec.mpjsons.impl.serializer.common

import pl.marpiec.mpjsons.JsonTypeSerializer
import pl.marpiec.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object IteratorSerializer extends JsonTypeSerializer {

  def serialize(obj: Any, jsonBuilder: StringBuilder) = {

    jsonBuilder.append('[')

    var isNotFirstField = false

    val iterator = obj.asInstanceOf[Iterator[_]]

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
