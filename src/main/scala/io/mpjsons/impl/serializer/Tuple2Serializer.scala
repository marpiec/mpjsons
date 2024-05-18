package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple2Serializer[T1, T2](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.Tuple2[T1, T2]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val t1Serializer = serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[T1]]
  private val t2Serializer = serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[T2]]

  override def serialize(tuple: scala.Tuple2[T1, T2], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('[')
    t1Serializer.serialize(tuple._1, jsonBuilder)
    jsonBuilder.append(',')
    t2Serializer.serialize(tuple._2, jsonBuilder)
    jsonBuilder.append(']')
  }

}
