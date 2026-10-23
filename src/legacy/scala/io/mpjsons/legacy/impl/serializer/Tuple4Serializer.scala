package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.JsonTypeSerializer
import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple4Serializer[T1, T2, T3, T4](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.Tuple4[T1, T2, T3, T4]] {

  private val subtypes = TypesUtil.getMultipleSubElementsType(tpe)
  private val t1Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes.head, context).asInstanceOf[JsonTypeSerializer[T1]], serializerFactory.config, "a value of " + tpe)
  private val t2Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(1), context).asInstanceOf[JsonTypeSerializer[T2]], serializerFactory.config, "a value of " + tpe)
  private val t3Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(2), context).asInstanceOf[JsonTypeSerializer[T3]], serializerFactory.config, "a value of " + tpe)
  private val t4Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes(3), context).asInstanceOf[JsonTypeSerializer[T4]], serializerFactory.config, "a value of " + tpe)

  override def serialize(tuple: scala.Tuple4[T1, T2, T3, T4], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('[')
    t1Serializer.serialize(tuple._1, jsonBuilder)
    jsonBuilder.append(',')
    t2Serializer.serialize(tuple._2, jsonBuilder)
    jsonBuilder.append(',')
    t3Serializer.serialize(tuple._3, jsonBuilder)
    jsonBuilder.append(',')
    t4Serializer.serialize(tuple._4, jsonBuilder)
    jsonBuilder.append(']')
  }

}