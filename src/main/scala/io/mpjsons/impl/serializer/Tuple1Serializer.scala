package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple1Serializer[T1](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.Tuple1[T1]] {

  private val subtype = TypesUtil.getSubElementsType(tpe)
  private val t1Serializer = serializerFactory.getSerializer(subtype, context).asInstanceOf[JsonTypeSerializer[T1]]

  override def serialize(tuple: scala.Tuple1[T1], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('[')
    t1Serializer.serialize(tuple._1, jsonBuilder)
    jsonBuilder.append(']')
  }
}