package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * Serializers for integral and boolean values that unbox and append the primitive directly,
 * avoiding the String that StringBuilder.append(Any) would create for every number.
 */
object IntSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(obj.asInstanceOf[Int])
}

object LongSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(obj.asInstanceOf[Long])
}

object ShortSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(obj.asInstanceOf[Short].toInt)
}

object ByteSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(obj.asInstanceOf[Byte].toInt)
}

object BooleanSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(if (obj.asInstanceOf[Boolean]) "true" else "false")
}
