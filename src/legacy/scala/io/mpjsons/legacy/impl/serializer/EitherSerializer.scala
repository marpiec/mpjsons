package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.JsonTypeSerializer
import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

class EitherSerializer[L, R](serializerFactory: SerializerFactory, tpe: Type, context: Context) extends JsonTypeSerializer[Either[L, R]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val leftSerializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[L]], serializerFactory.config, "a value of " + tpe)
  private val rightSerializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[R]], serializerFactory.config, "a value of " + tpe)

  override def serialize(obj: Either[L, R], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('{')

    obj match {
      case Left(value) =>
        jsonBuilder.append("\"left\":")
        leftSerializer.serialize(value, jsonBuilder)
      case Right(value) =>
        jsonBuilder.append("\"right\":")
        rightSerializer.serialize(value, jsonBuilder)
    }

    jsonBuilder.append('}')

  }
}