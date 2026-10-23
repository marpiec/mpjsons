package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

class EitherSerializer[L, R](serializerFactory: SerializerFactory, tpe: Type, context: Context) extends JsonTypeSerializer[Either[L, R]] with Utf8Writer[Either[L, R]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val leftSerializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[L]], serializerFactory.config, "a value of " + tpe)
  private val rightSerializer = NullValues.nullSafe(serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[R]], serializerFactory.config, "a value of " + tpe)
  private val leftWriter = Utf8Writer.of(leftSerializer)
  private val rightWriter = Utf8Writer.of(rightSerializer)

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

  override def writeUtf8(obj: Either[L, R], out: Utf8Output): Unit = {

    out.writeByte('{')

    obj match {
      case Left(value) =>
        out.writeAscii("\"left\":")
        leftWriter.writeUtf8(value, out)
      case Right(value) =>
        out.writeAscii("\"right\":")
        rightWriter.writeUtf8(value, out)
    }

    out.writeByte('}')

  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[EitherSerializer[_, _]]
}
