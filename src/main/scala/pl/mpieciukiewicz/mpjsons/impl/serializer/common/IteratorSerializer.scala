package pl.mpieciukiewicz.mpjsons.impl.serializer.common

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object IteratorSerializer extends JsonTypeSerializer[Iterator[_]] {

  override def serialize(iterator: Iterator[_], jsonBuilder: StringBuilder) = {

    jsonBuilder.append('[')

    var isNotFirstField = false

    iterator.foreach(element => {

      if (isNotFirstField) {
        jsonBuilder.append(",")
      } else {
        isNotFirstField = true
      }
      SerializerFactory.getSerializer(element.getClass).asInstanceOf[JsonTypeSerializer[Any]].serialize(element, jsonBuilder)
    })


    jsonBuilder.append(']')
  }


}
