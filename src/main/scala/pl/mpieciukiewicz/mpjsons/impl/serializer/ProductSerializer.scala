package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil


/**
 * @author Marcin Pieciukiewicz
 */

object ProductSerializer extends JsonTypeSerializer[Product] {

  override def serialize(tuple: Product, jsonBuilder: StringBuilder)(implicit serializerFactory: SerializerFactory) = {

    jsonBuilder.append('[')

    var isNotFirstField = false

    val iterator = tuple.productIterator

    iterator.foreach(element => {
      if (isNotFirstField) {
        jsonBuilder.append(',')
      } else {
        isNotFirstField = true
      }
      serializerFactory.getSerializer(TypesUtil.getTypeFromClass(element.getClass)).asInstanceOf[JsonTypeSerializer[Any]].serialize(element, jsonBuilder)
    })


    jsonBuilder.append(']')
  }
}
