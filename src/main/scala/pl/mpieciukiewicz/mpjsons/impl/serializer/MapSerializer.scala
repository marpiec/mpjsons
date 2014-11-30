package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object MapSerializer extends JsonTypeSerializer {


  def serialize(obj: Any, jsonBuilder: StringBuilder) = {

    val map: scala.collection.Map[_, _] = obj.asInstanceOf[scala.collection.Map[_, _]]

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
