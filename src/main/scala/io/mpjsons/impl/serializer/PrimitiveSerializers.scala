package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * Serializers for integral and boolean values that unbox and append the primitive directly,
 * avoiding the String that StringBuilder.append(Any) would create for every number.
 */
object IntSerializer extends JsonTypeSerializer[Any] with Utf8Writer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(obj.asInstanceOf[Int])
  override def writeUtf8(obj: Any, out: Utf8Output): Unit = out.writeInt(obj.asInstanceOf[Int])
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}

object LongSerializer extends JsonTypeSerializer[Any] with Utf8Writer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(obj.asInstanceOf[Long])
  override def writeUtf8(obj: Any, out: Utf8Output): Unit = out.writeLong(obj.asInstanceOf[Long])
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}

object ShortSerializer extends JsonTypeSerializer[Any] with Utf8Writer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(obj.asInstanceOf[Short].toInt)
  override def writeUtf8(obj: Any, out: Utf8Output): Unit = out.writeInt(obj.asInstanceOf[Short].toInt)
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}

object ByteSerializer extends JsonTypeSerializer[Any] with Utf8Writer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(obj.asInstanceOf[Byte].toInt)
  override def writeUtf8(obj: Any, out: Utf8Output): Unit = out.writeInt(obj.asInstanceOf[Byte].toInt)
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}

object BooleanSerializer extends JsonTypeSerializer[Any] with Utf8Writer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = jsonBuilder.underlying.append(if (obj.asInstanceOf[Boolean]) "true" else "false")
  override def writeUtf8(obj: Any, out: Utf8Output): Unit = out.writeAscii(if (obj.asInstanceOf[Boolean]) "true" else "false")
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
