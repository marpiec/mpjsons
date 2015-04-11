package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import scala.reflect.runtime.universe._


/**
 * @author Marcin Pieciukiewicz
 */

object ProductSerializer extends JsonTypeSerializer[Product] {

  override def serialize(tuple: Product, tpe: Type, jsonBuilder: StringBuilder) = {

    jsonBuilder.append('[')

    var isNotFirstField = false

    val iterator = tuple.productIterator

    iterator.foreach(element => {
      if (isNotFirstField) {
        jsonBuilder.append(",")
      } else {
        isNotFirstField = true
      }
      SerializerFactory.getSerializer(element.getClass).serialize(element, jsonBuilder)
    })


    jsonBuilder.append(']')
  }
}
