package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import io.mpjsons.impl.util.TypesUtil

import scala.collection.BitSet
import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */

class BitSetSerializer(serializerFactory: SerializerFactory, tpe: Type, context: Context) extends JsonTypeSerializer[BitSet] {

  val subTypeSerializer = serializerFactory.getSerializer[Int](typeOf[Int], context)



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













