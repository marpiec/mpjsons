package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.serializer.{Utf8Output, Utf8Writer}

import java.time.LocalDateTime

object LocalDateTimeSerializer extends JsonTypeSerializer[LocalDateTime] with Utf8Writer[LocalDateTime] {

  override def serialize(obj: LocalDateTime, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{\"date\":")
    LocalDateSerializer.serialize(obj.toLocalDate, jsonBuilder)
    jsonBuilder.append(",\"time\":")
    LocalTimeSerializer.serialize(obj.toLocalTime, jsonBuilder)
    jsonBuilder.append('}')
  }

  override def writeUtf8(obj: LocalDateTime, out: Utf8Output): Unit = {
    out.writeAscii("{\"date\":")
    LocalDateSerializer.writeUtf8(obj.toLocalDate, out)
    out.writeAscii(",\"time\":")
    LocalTimeSerializer.writeUtf8(obj.toLocalTime, out)
    out.writeByte('}')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
