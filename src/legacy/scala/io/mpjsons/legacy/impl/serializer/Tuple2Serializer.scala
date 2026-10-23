package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.JsonTypeSerializer
import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple2Serializer[T1, T2](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.Tuple2[T1, T2]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val t1Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[T1]], serializerFactory.config, "a value of " + tpe)
  private val t2Serializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[T2]], serializerFactory.config, "a value of " + tpe)

  override def serialize(tuple: scala.Tuple2[T1, T2], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('[')
    t1Serializer.serialize(tuple._1, jsonBuilder)
    jsonBuilder.append(',')
    t2Serializer.serialize(tuple._2, jsonBuilder)
    jsonBuilder.append(']')
  }

}
