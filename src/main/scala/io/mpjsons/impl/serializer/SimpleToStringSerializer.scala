package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object SimpleToStringSerializer extends JsonTypeSerializer[Any] with Utf8Writer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append(obj)
  }

  override def writeUtf8(obj: Any, out: Utf8Output): Unit = {
    out.writeText(String.valueOf(obj))
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
