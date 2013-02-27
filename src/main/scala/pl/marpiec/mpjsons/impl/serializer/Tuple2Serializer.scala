package pl.marpiec.mpjsons.impl.serializer

import pl.marpiec.mpjsons.JsonTypeSerializer
import pl.marpiec.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object Tuple2Serializer extends JsonTypeSerializer {

  def serialize(obj: Any, jsonBuilder: StringBuilder) = {

    val tuple = obj.asInstanceOf[Tuple2[_, _]]
    val first = tuple._1
    val second = tuple._2

    jsonBuilder.append('[')
    SerializerFactory.getSerializer(first).serialize(first, jsonBuilder)
    jsonBuilder.append(',')
    SerializerFactory.getSerializer(second).serialize(second, jsonBuilder)
    jsonBuilder.append(']')
  }
}
