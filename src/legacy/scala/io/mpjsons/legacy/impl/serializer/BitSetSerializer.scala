package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.JsonTypeSerializer
import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.util.Context

import scala.collection.BitSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BitSetSerializer(serializerFactory: SerializerFactory, tpe: Type, context: Context) extends JsonTypeSerializer[BitSet] {

  private val subTypeSerializer = serializerFactory.getSerializer[Int](typeOf[Int], context)


  override def serialize(obj: BitSet, jsonBuilder: StringBuilder): Unit = {

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













