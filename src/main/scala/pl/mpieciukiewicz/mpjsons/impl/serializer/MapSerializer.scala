package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object MapSerializer extends JsonTypeSerializer[scala.collection.Map[_, _]] {


  override def serialize(map: scala.collection.Map[_, _], jsonBuilder: StringBuilder) = {


    jsonBuilder.append('[')
    var nonFirstField = false

    for ((key, value) <- map) {
      if (nonFirstField) {
        jsonBuilder.append(",")
      } else {
        nonFirstField = true
      }
      jsonBuilder.append("[")

      SerializerFactory.getSerializer(TypesUtil.getTypeFromClass(key.getClass)).asInstanceOf[JsonTypeSerializer[Any]].serialize(key, jsonBuilder)
      jsonBuilder.append(",")
      SerializerFactory.getSerializer(TypesUtil.getTypeFromClass(value.getClass)).asInstanceOf[JsonTypeSerializer[Any]].serialize(value, jsonBuilder)
      jsonBuilder.append("]")
    }

    jsonBuilder.append(']')
  }

}
