package pl.marpiec.mpjsons.impl.serializer

import pl.marpiec.mpjsons.JsonTypeSerializer
import pl.marpiec.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object MapSerializer extends JsonTypeSerializer {


  def serialize(obj: Any, jsonBuilder: StringBuilder) = {

    jsonBuilder.append('[')

    val map: Map[_, _] = obj.asInstanceOf[Map[_, _]]

    var isNotFirstField = false

    for ((key, value) <- map) {
      if (isNotFirstField) {
        jsonBuilder.append(",")
      } else {
        isNotFirstField = true
      }
      jsonBuilder.append("{k:")
      SerializerFactory.getSerializer(key).serialize(key, jsonBuilder)
      jsonBuilder.append(",v:")
      SerializerFactory.getSerializer(value).serialize(value, jsonBuilder)
      jsonBuilder.append("}");
    }

    jsonBuilder.append(']')
  }


}
