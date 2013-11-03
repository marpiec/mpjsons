package pl.marpiec.mpjsons.impl.serializer

import pl.marpiec.mpjsons.JsonTypeSerializer
import pl.marpiec.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object MapSerializer extends JsonTypeSerializer {


  def serialize(obj: Any, jsonBuilder: StringBuilder) = {

    val map: Map[_, _] = obj.asInstanceOf[Map[_, _]]

    jsonBuilder.append('[')
    var nonFirstField = false

    for ((key, value) <- map) {
      if (nonFirstField) {
        jsonBuilder.append(",")
      } else {
        nonFirstField = true
      }
      jsonBuilder.append("[")
      SerializerFactory.getSerializer(key.getClass).serialize(key, jsonBuilder)
      jsonBuilder.append(",")
      SerializerFactory.getSerializer(value.getClass).serialize(value, jsonBuilder)
      jsonBuilder.append("]")
    }

    jsonBuilder.append(']')
  }

}
