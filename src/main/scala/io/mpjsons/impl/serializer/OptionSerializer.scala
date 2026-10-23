package io.mpjsons.impl.serializer

import io.mpjsons.{JsonTypeSerializer, NullPolicy}
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * Historical form: None -> {}, Some(v) -> {"value":v}.
 * Compact form (`compactOptions`): None -> null, Some(v) -> v. An Option nested directly inside another Option
 * always keeps the historical form so that Some(None) stays distinguishable from None.
 * Some(null) is written with a null payload only when the NullPolicy allows nulls; under Fail it is rejected.
 */
class OptionSerializer[T](serializerFactory: SerializerFactory, private val tpe: Type, private val context: Context, compact: Boolean)
  extends JsonTypeSerializer[Option[T]] with Utf8Writer[Option[T]] {

  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context) = this(serializerFactory, tpe, context, false)

  private val subType = TypesUtil.getSubElementsType(tpe)
  private val subTypeSerializer: JsonTypeSerializer[T] =
    OptionSerializer.elementSerializer(serializerFactory, subType, context, compact).asInstanceOf[JsonTypeSerializer[T]]
  private val nullsAllowed = serializerFactory.config.nullPolicy != NullPolicy.Fail

  override def serialize(obj: Option[T], jsonBuilder: StringBuilder): Unit = {
    if (compact) {
      obj match {
        case Some(value) => if (value == null) NullValues.writeNull(jsonBuilder, nullsAllowed, "Some of " + tpe) else subTypeSerializer.serialize(value, jsonBuilder)
        case None => jsonBuilder.append("null")
      }
    } else {
      jsonBuilder.append('{')
      obj match {
        case Some(value) =>
          jsonBuilder.append("\"value\":")
          if (value == null) {
            NullValues.writeNull(jsonBuilder, nullsAllowed, "Some of " + tpe)
          } else {
            subTypeSerializer.serialize(value, jsonBuilder)
          }
        case None => ()
      }
      jsonBuilder.append('}')
    }
  }

  private val subTypeWriter: Utf8Writer[T] = Utf8Writer.of(subTypeSerializer)

  override def writeUtf8(obj: Option[T], out: Utf8Output): Unit = {
    if (compact) {
      obj match {
        case Some(value) => if (value == null) NullValues.writeNull(out, nullsAllowed, "Some of " + tpe) else subTypeWriter.writeUtf8(value, out)
        case None => out.writeAscii("null")
      }
    } else {
      out.writeByte('{')
      obj match {
        case Some(value) =>
          out.writeAscii("\"value\":")
          if (value == null) {
            NullValues.writeNull(out, nullsAllowed, "Some of " + tpe)
          } else {
            subTypeWriter.writeUtf8(value, out)
          }
        case None => ()
      }
      out.writeByte('}')
    }
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[OptionSerializer[_]]
}

object OptionSerializer {
  private val optionType = typeOf[Option[_]]

  /** The serializer for an Option's element: a nested Option keeps the historical form when `compact` is on. */
  def elementSerializer(serializerFactory: SerializerFactory, elementType: Type, context: Context, compact: Boolean): JsonTypeSerializer[AnyRef] = {
    if (compact && elementType <:< optionType) {
      new OptionSerializer[Any](serializerFactory, elementType, context, false).asInstanceOf[JsonTypeSerializer[AnyRef]]
    } else {
      serializerFactory.getSerializer(elementType, context).asInstanceOf[JsonTypeSerializer[AnyRef]]
    }
  }
}
