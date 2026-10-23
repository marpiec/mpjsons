package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple1Serializer[T1](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.Tuple1[T1]] with Utf8Writer[scala.Tuple1[T1]] {

  private val subtype = TypesUtil.getSubElementsType(tpe)
  private val t1Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtype, context).asInstanceOf[JsonTypeSerializer[T1]], serializerFactory.config, "a value of " + tpe)
  private val t1Writer = Utf8Writer.of(t1Serializer)

  override def serialize(tuple: scala.Tuple1[T1], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('[')
    t1Serializer.serialize(tuple._1, jsonBuilder)
    jsonBuilder.append(']')
  }

  override def writeUtf8(tuple: scala.Tuple1[T1], out: Utf8Output): Unit = {
    out.writeByte('[')
    t1Writer.writeUtf8(tuple._1, out)
    out.writeByte(']')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[Tuple1Serializer[_]]
}