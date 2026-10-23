package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.Context

import scala.collection.BitSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BitSetSerializer(serializerFactory: SerializerFactory, tpe: Type, context: Context) extends JsonTypeSerializer[BitSet] with Utf8Writer[BitSet] {

  private val subTypeSerializer = serializerFactory.getSerializer[Int](typeOf[Int], context)
  private val subTypeWriter = Utf8Writer.of(subTypeSerializer)

  override def writeUtf8(obj: BitSet, out: Utf8Output): Unit = {

    out.writeByte('[')

    var isNotFirstField = false

    obj.iterator.foreach(element => {

      if (isNotFirstField) {
        out.writeByte(',')
      } else {
        isNotFirstField = true
      }
      subTypeWriter.writeUtf8(element, out)
    })


    out.writeByte(']')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[BitSetSerializer]


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













