package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, TypesUtil}

import scala.reflect.runtime.universe._

class EitherSerializer[L, R](serializerFactory: SerializerFactory, tpe: Type, context: Context) extends JsonTypeSerializer[Either[L, R]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val leftSerializer = serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[L]]
  private val rightSerializer = serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[R]]

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