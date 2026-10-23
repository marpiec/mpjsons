package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.serializer.{Utf8Output, Utf8Writer}

import java.time.LocalTime

object LocalTimeSerializer extends JsonTypeSerializer[LocalTime] with Utf8Writer[LocalTime] {

  override def serialize(obj: LocalTime, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{\"hour\":")
    jsonBuilder.append(obj.getHour)
    jsonBuilder.append(",\"minute\":")
    jsonBuilder.append(obj.getMinute)
    jsonBuilder.append(",\"second\":")
    jsonBuilder.append(obj.getSecond)
    jsonBuilder.append(",\"nano\":")
    jsonBuilder.append(obj.getNano)
    jsonBuilder.append('}')
  }

  override def writeUtf8(obj: LocalTime, out: Utf8Output): Unit = {
    out.writeAscii("{\"hour\":")
    out.writeInt(obj.getHour)
    out.writeAscii(",\"minute\":")
    out.writeInt(obj.getMinute)
    out.writeAscii(",\"second\":")
    out.writeInt(obj.getSecond)
    out.writeAscii(",\"nano\":")
    out.writeInt(obj.getNano)
    out.writeByte('}')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
