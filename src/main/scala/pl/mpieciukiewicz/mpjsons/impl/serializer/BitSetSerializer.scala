package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.serializer.common.IteratorSerializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

import scala.collection.BitSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BitSetSerializer(serializerFactory: SerializerFactory, tpe: Type) extends JsonTypeSerializer[BitSet] {

  val subTypeSerializer = serializerFactory.getSerializer[Int](typeOf[Int])



  override def serialize(obj: BitSet, jsonBuilder: StringBuilder) {

    jsonBuilder.append('[')

    var isNotFirstField = false

    obj.iterator.foreach(element => {

      if (isNotFirstField) {
        jsonBuilder.append(',')
      } else {
        isNotFirstField = true
      }
      subTypeSerializer.serialize(element, jsonBuilder)
    })


    jsonBuilder.append(']')
  }

}













