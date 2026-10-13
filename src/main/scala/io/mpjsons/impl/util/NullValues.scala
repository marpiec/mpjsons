package io.mpjsons.impl.util

import io.mpjsons.impl.StringIterator
import io.mpjsons.{JsonTypeDeserializer, JsonTypeSerializer}

import scala.reflect.runtime.universe._

/**
 * What a JSON `null` means for one Scala type: `None` for Option, `null` for any other reference type,
 * and an error for primitives (Int, Boolean, ...), which cannot hold it.
 */
final class NullValue private[util] (val allowed: Boolean, val value: AnyRef, description: String) {

  /** The Scala value for a JSON null, or an IllegalArgumentException naming the type when null is not allowed. */
  def get(): AnyRef = {
    if (allowed) {
      value
    } else {
      throw new IllegalArgumentException("JSON null is not allowed for " + description)
    }
  }
}

object NullValues {

  private val optionType = typeOf[Option[_]]
  private val anyRefType = typeOf[AnyRef]

  def forType(tpe: Type): NullValue = make(tpe, tpe.toString)

  def forField(tpe: Type, fieldName: String, ownerTypeName: String): NullValue =
    make(tpe, "field '" + fieldName + "' of type " + tpe + " in " + ownerTypeName)

  private def make(tpe: Type, description: String): NullValue = {
    if (tpe <:< optionType) {
      new NullValue(true, None, description)
    } else if (tpe <:< anyRefType) {
      new NullValue(true, null, description)
    } else {
      new NullValue(false, null, "primitive " + description)
    }
  }

  /** Reads a value that may be the `null` literal. */
  def read[T](jsonIterator: StringIterator, nullValue: NullValue, deserializer: JsonTypeDeserializer[T]): T = {
    if (jsonIterator.tryConsumeNull()) {
      nullValue.get().asInstanceOf[T]
    } else {
      deserializer.deserialize(jsonIterator)
    }
  }

  /** Wraps a deserializer so that the `null` literal maps to the type's null value. */
  def nullable[T](deserializer: JsonTypeDeserializer[T], tpe: Type): JsonTypeDeserializer[T] =
    new NullableDeserializer[T](deserializer, forType(tpe))

  /** Wraps a serializer so that a null value is written as the `null` literal. */
  def nullSafe[T](serializer: JsonTypeSerializer[T]): JsonTypeSerializer[T] = new NullSafeSerializer[T](serializer)
}

final class NullableDeserializer[T](inner: JsonTypeDeserializer[T], nullValue: NullValue) extends JsonTypeDeserializer[T] {
  override def deserialize(jsonIterator: StringIterator): T = NullValues.read(jsonIterator, nullValue, inner)
}

final class NullSafeSerializer[T](inner: JsonTypeSerializer[T]) extends JsonTypeSerializer[T] {
  override def serialize(obj: T, jsonBuilder: StringBuilder): Unit = {
    if (obj == null) {
      jsonBuilder.append("null")
    } else {
      inner.serialize(obj, jsonBuilder)
    }
  }
}
