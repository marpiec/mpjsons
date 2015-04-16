package pl.mpieciukiewicz.mpjsons.impl.serializer.common

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object IteratorSerializer extends JsonTypeSerializer[Iterator[_]] {

  override def serialize(iterator: Iterator[_], jsonBuilder: StringBuilder)(implicit serializerFactory: SerializerFactory) = {

    jsonBuilder.append('[')

    var isNotFirstField = false

    iterator.foreach(element => {

      if (isNotFirstField) {
        jsonBuilder.append(",")
      } else {
        isNotFirstField = true
      }
      serializerFactory.getSerializer(TypesUtil.getTypeFromClass(element.getClass)).asInstanceOf[JsonTypeSerializer[Any]].serialize(element, jsonBuilder)
    })


    jsonBuilder.append(']')
  }


}
