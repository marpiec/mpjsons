package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

object SingletonObjectSerializer extends JsonTypeSerializer[AnyRef] with Utf8Writer[AnyRef] {

  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{}")
  }

  override def writeUtf8(obj: AnyRef, out: Utf8Output): Unit = {
    out.writeAscii("{}")
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
