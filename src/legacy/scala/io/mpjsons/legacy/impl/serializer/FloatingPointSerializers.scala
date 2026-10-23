package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.JsonTypeSerializer

/**
 * Serializers for Double and Float (Scala and java.lang boxed values).
 * JSON has no representation for NaN or infinities, so those values are rejected instead of
 * producing output that no JSON parser (including this library) can read back.
 */
object DoubleSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    val value = obj.asInstanceOf[Double]
    if (java.lang.Double.isFinite(value)) {
      jsonBuilder.append(value)
    } else {
      throw new IllegalArgumentException("Non-finite floating point value " + value + " cannot be represented in JSON")
    }
  }
}

object FloatSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    val value = obj.asInstanceOf[Float]
    if (java.lang.Float.isFinite(value)) {
      jsonBuilder.append(value)
    } else {
      throw new IllegalArgumentException("Non-finite floating point value " + value + " cannot be represented in JSON")
    }
  }
}
