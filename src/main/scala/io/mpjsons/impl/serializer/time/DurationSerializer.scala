package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.serializer.{Utf8Output, Utf8Writer}

import java.time.Duration

object DurationSerializer extends JsonTypeSerializer[Duration] with Utf8Writer[Duration] {

  override def serialize(obj: Duration, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{\"seconds\":")
    jsonBuilder.append(obj.getSeconds)
    jsonBuilder.append(",\"nanos\":")
    jsonBuilder.append(obj.getNano)
    jsonBuilder.append('}')
  }

  override def writeUtf8(obj: Duration, out: Utf8Output): Unit = {
    out.writeAscii("{\"seconds\":")
    out.writeLong(obj.getSeconds)
    out.writeAscii(",\"nanos\":")
    out.writeInt(obj.getNano)
    out.writeByte('}')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
