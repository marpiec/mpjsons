package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

/**
 * @author Marcin Pieciukiewicz
 */

case class MapSerializer[K,V](serializerFactory: SerializerFactory)
  extends JsonTypeSerializer[scala.collection.Map[K, V]] {


  override def serialize(map: scala.collection.Map[K, V], jsonBuilder: StringBuilder) = {


    jsonBuilder.append('[')
    var nonFirstField = false

    for ((key: K, value: V) <- map) {
      if (nonFirstField) {
        jsonBuilder.append(',')
      } else {
        nonFirstField = true
      }
      jsonBuilder.append('[')

      serializerFactory.getSerializer(TypesUtil.getTypeFromClass(key.getClass))
        .asInstanceOf[JsonTypeSerializer[Any]].serialize(key, jsonBuilder)

      jsonBuilder.append(',')

      serializerFactory.getSerializer(TypesUtil.getTypeFromClass(value.getClass))
        .asInstanceOf[JsonTypeSerializer[Any]].serialize(value, jsonBuilder)

      jsonBuilder.append(']')
    }

    jsonBuilder.append(']')
  }

}
