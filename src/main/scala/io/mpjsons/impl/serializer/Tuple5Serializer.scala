package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple5Serializer[T1, T2, T3, T4, T5](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.Tuple5[T1, T2, T3, T4, T5]] with Utf8Writer[scala.Tuple5[T1, T2, T3, T4, T5]] {

  private val subtypes = TypesUtil.getMultipleSubElementsType(tpe)
  private val t1Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes.head, context).asInstanceOf[JsonTypeSerializer[T1]], serializerFactory.config, "a value of " + tpe)
  private val t2Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(1), context).asInstanceOf[JsonTypeSerializer[T2]], serializerFactory.config, "a value of " + tpe)
  private val t3Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(2), context).asInstanceOf[JsonTypeSerializer[T3]], serializerFactory.config, "a value of " + tpe)
  private val t4Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(3), context).asInstanceOf[JsonTypeSerializer[T4]], serializerFactory.config, "a value of " + tpe)
  private val t5Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(4), context).asInstanceOf[JsonTypeSerializer[T5]], serializerFactory.config, "a value of " + tpe)

  override def serialize(tuple: scala.Tuple5[T1, T2, T3, T4, T5], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('[')
    t1Serializer.serialize(tuple._1, jsonBuilder)
    jsonBuilder.append(',')
    t2Serializer.serialize(tuple._2, jsonBuilder)
    jsonBuilder.append(',')
    t3Serializer.serialize(tuple._3, jsonBuilder)
    jsonBuilder.append(',')
    t4Serializer.serialize(tuple._4, jsonBuilder)
    jsonBuilder.append(',')
    t5Serializer.serialize(tuple._5, jsonBuilder)
    jsonBuilder.append(']')
  }

  private val t1Writer = Utf8Writer.of(t1Serializer)
  private val t2Writer = Utf8Writer.of(t2Serializer)
  private val t3Writer = Utf8Writer.of(t3Serializer)
  private val t4Writer = Utf8Writer.of(t4Serializer)
  private val t5Writer = Utf8Writer.of(t5Serializer)

  override def writeUtf8(tuple: scala.Tuple5[T1, T2, T3, T4, T5], out: Utf8Output): Unit = {
    out.writeByte('[')
    t1Writer.writeUtf8(tuple._1, out)
    out.writeByte(',')
    t2Writer.writeUtf8(tuple._2, out)
    out.writeByte(',')
    t3Writer.writeUtf8(tuple._3, out)
    out.writeByte(',')
    t4Writer.writeUtf8(tuple._4, out)
    out.writeByte(',')
    t5Writer.writeUtf8(tuple._5, out)
    out.writeByte(']')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[Tuple5Serializer[_, _, _, _, _]]

}