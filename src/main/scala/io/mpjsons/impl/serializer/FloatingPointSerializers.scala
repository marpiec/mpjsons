package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * Serializers for Double and Float (Scala and java.lang boxed values).
 * JSON has no representation for NaN or infinities, so those values are rejected instead of
 * producing output that no JSON parser (including this library) can read back.
 */
object DoubleSerializer extends JsonTypeSerializer[Any] with Utf8Writer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    val value = obj.asInstanceOf[Double]
    if (java.lang.Double.isFinite(value)) {
      jsonBuilder.append(value)
    } else {
      throw nonFinite(value)
    }
  }

  override def writeUtf8(obj: Any, out: Utf8Output): Unit = {
    val value = obj.asInstanceOf[Double]
    if (java.lang.Double.isFinite(value)) {
      out.writeDouble(value)
    } else {
      throw nonFinite(value)
    }
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true

  private[serializer] def nonFinite(value: Double): IllegalArgumentException =
    new IllegalArgumentException("Non-finite floating point value " + value + " cannot be represented in JSON")
}

object FloatSerializer extends JsonTypeSerializer[Any] with Utf8Writer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    val value = obj.asInstanceOf[Float]
    if (java.lang.Float.isFinite(value)) {
      jsonBuilder.append(value)
    } else {
      throw nonFinite(value)
    }
  }

  override def writeUtf8(obj: Any, out: Utf8Output): Unit = {
    val value = obj.asInstanceOf[Float]
    if (java.lang.Float.isFinite(value)) {
      out.writeFloat(value)
    } else {
      throw nonFinite(value)
    }
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true

  private[serializer] def nonFinite(value: Float): IllegalArgumentException =
    new IllegalArgumentException("Non-finite floating point value " + value + " cannot be represented in JSON")
}
