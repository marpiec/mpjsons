package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple3Serializer[T1, T2, T3](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.Tuple3[T1, T2, T3]] with Utf8Writer[scala.Tuple3[T1, T2, T3]] {

  private val subtypes = TypesUtil.getMultipleSubElementsType(tpe)
  private val t1Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(0), context).asInstanceOf[JsonTypeSerializer[T1]], serializerFactory.config, "a value of " + tpe)
  private val t2Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(1), context).asInstanceOf[JsonTypeSerializer[T2]], serializerFactory.config, "a value of " + tpe)
  private val t3Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(2), context).asInstanceOf[JsonTypeSerializer[T3]], serializerFactory.config, "a value of " + tpe)

  override def serialize(tuple: scala.Tuple3[T1, T2, T3], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('[')
    t1Serializer.serialize(tuple._1, jsonBuilder)
    jsonBuilder.append(',')
    t2Serializer.serialize(tuple._2, jsonBuilder)
    jsonBuilder.append(',')
    t3Serializer.serialize(tuple._3, jsonBuilder)
    jsonBuilder.append(']')
  }

  private val t1Writer = Utf8Writer.of(t1Serializer)
  private val t2Writer = Utf8Writer.of(t2Serializer)
  private val t3Writer = Utf8Writer.of(t3Serializer)

  override def writeUtf8(tuple: scala.Tuple3[T1, T2, T3], out: Utf8Output): Unit = {
    out.writeByte('[')
    t1Writer.writeUtf8(tuple._1, out)
    out.writeByte(',')
    t2Writer.writeUtf8(tuple._2, out)
    out.writeByte(',')
    t3Writer.writeUtf8(tuple._3, out)
    out.writeByte(']')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[Tuple3Serializer[_, _, _]]

}
