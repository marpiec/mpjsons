package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple2Serializer[T1, T2](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.Tuple2[T1, T2]] with Utf8Writer[scala.Tuple2[T1, T2]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val t1Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[T1]], serializerFactory.config, "a value of " + tpe)
  private val t2Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[T2]], serializerFactory.config, "a value of " + tpe)
  private val t1Writer = Utf8Writer.of(t1Serializer)
  private val t2Writer = Utf8Writer.of(t2Serializer)

  override def serialize(tuple: scala.Tuple2[T1, T2], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('[')
    t1Serializer.serialize(tuple._1, jsonBuilder)
    jsonBuilder.append(',')
    t2Serializer.serialize(tuple._2, jsonBuilder)
    jsonBuilder.append(']')
  }

  override def writeUtf8(tuple: scala.Tuple2[T1, T2], out: Utf8Output): Unit = {
    out.writeByte('[')
    t1Writer.writeUtf8(tuple._1, out)
    out.writeByte(',')
    t2Writer.writeUtf8(tuple._2, out)
    out.writeByte(']')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[Tuple2Serializer[_, _]]

}
