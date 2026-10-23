package io.mpjsons.impl.util

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.serializer.{Utf8Output, Utf8Writer}
import io.mpjsons.{JsonTypeDeserializer, JsonTypeSerializer, MPJsonsConfig, NullPolicy}

import scala.reflect.runtime.universe._

/**
 * What a JSON `null` means for one value: `None` for Option, `null` for a reference type that is declared
 * nullable (a field annotated with `@nullable`, or any reference type when the NullPolicy is not Fail), and an
 * error otherwise (primitives cannot hold it; undeclared references are rejected under NullPolicy.Fail).
 */
final class NullValue private[util] (val allowed: Boolean, val value: AnyRef, description: String) {

  /** The Scala value for a JSON null, or an IllegalArgumentException when null is not allowed here. */
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

  /** Nullability of a standalone value: an element, map key or value, tuple slot, Either payload, Some payload, root. */
  def forType(tpe: Type, config: MPJsonsConfig): NullValue =
    make(tpe, "a value of type " + tpe, declaredNullable = false, config)

  /** Nullability of a bean field: `@nullable` declares it. */
  def forField(tpe: Type, fieldName: String, ownerTypeName: String, declaredNullable: Boolean, config: MPJsonsConfig): NullValue =
    make(tpe, "field '" + fieldName + "' of type " + tpe + " in " + ownerTypeName, declaredNullable, config)

  private def make(tpe: Type, description: String, declaredNullable: Boolean, config: MPJsonsConfig): NullValue = {
    if (tpe <:< optionType) {
      new NullValue(true, None, description)
    } else if (!(tpe <:< anyRefType)) {
      new NullValue(false, null, "primitive " + description)
    } else if (declaredNullable || config.nullPolicy != NullPolicy.Fail) {
      new NullValue(true, null, description)
    } else {
      new NullValue(false, null, description + " (declare it with @nullable or Option, or use a NullPolicy other than Fail)")
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

  /** Wraps a deserializer so that the `null` literal maps to the type's null value (or fails). */
  def nullable[T](deserializer: JsonTypeDeserializer[T], tpe: Type, config: MPJsonsConfig): JsonTypeDeserializer[T] =
    new NullableDeserializer[T](deserializer, forType(tpe, config))

  /** Wraps a serializer so that a null value is written as `null`, or rejected under NullPolicy.Fail. */
  def nullSafe[T](serializer: JsonTypeSerializer[T], config: MPJsonsConfig, where: String): JsonTypeSerializer[T] =
    new NullSafeSerializer[T](serializer, config.nullPolicy != NullPolicy.Fail, where)

  /** Writes `null`, or fails under NullPolicy.Fail; `where` names the place for the error message. */
  def writeNull(jsonBuilder: StringBuilder, nullsAllowed: Boolean, where: String): Unit = {
    if (nullsAllowed) {
      jsonBuilder.append("null")
    } else {
      throw nullNotAllowed(where)
    }
  }

  /** writeNull for UTF-8 output. */
  def writeNull(out: Utf8Output, nullsAllowed: Boolean, where: String): Unit = {
    if (nullsAllowed) {
      out.writeAscii("null")
    } else {
      throw nullNotAllowed(where)
    }
  }

  private def nullNotAllowed(where: String): IllegalArgumentException =
    new IllegalArgumentException("Null value is not allowed in " + where + " (NullPolicy.Fail); use NullPolicy.Omit or WriteNull to allow nulls")
}

final class NullableDeserializer[T](inner: JsonTypeDeserializer[T], nullValue: NullValue) extends JsonTypeDeserializer[T] {
  override def deserialize(jsonIterator: StringIterator): T = NullValues.read(jsonIterator, nullValue, inner)
}

final class NullSafeSerializer[T](inner: JsonTypeSerializer[T], nullsAllowed: Boolean, where: String) extends JsonTypeSerializer[T] with Utf8Writer[T] {

  private val innerWriter = Utf8Writer.of(inner)

  override def serialize(obj: T, jsonBuilder: StringBuilder): Unit = {
    if (obj == null) {
      NullValues.writeNull(jsonBuilder, nullsAllowed, where)
    } else {
      inner.serialize(obj, jsonBuilder)
    }
  }

  override def writeUtf8(obj: T, out: Utf8Output): Unit = {
    if (obj == null) {
      NullValues.writeNull(out, nullsAllowed, where)
    } else {
      innerWriter.writeUtf8(obj, out)
    }
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
