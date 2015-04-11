package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object MapSerializer extends JsonTypeSerializer[scala.collection.Map[_, _]] {


  override def serialize(map: scala.collection.Map[_, _], tpe: Type, jsonBuilder: StringBuilder) = {


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
