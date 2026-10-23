package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.serializer.{Utf8Output, Utf8Writer}

import java.time.LocalDate

object LocalDateSerializer extends JsonTypeSerializer[LocalDate] with Utf8Writer[LocalDate] {

  override def serialize(obj: LocalDate, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{\"year\":")
    jsonBuilder.append(obj.getYear)
    jsonBuilder.append(",\"month\":")
    jsonBuilder.append(obj.getMonthValue)
    jsonBuilder.append(",\"day\":")
    jsonBuilder.append(obj.getDayOfMonth)
    jsonBuilder.append('}')
  }

  override def writeUtf8(obj: LocalDate, out: Utf8Output): Unit = {
    out.writeAscii("{\"year\":")
    out.writeInt(obj.getYear)
    out.writeAscii(",\"month\":")
    out.writeInt(obj.getMonthValue)
    out.writeAscii(",\"day\":")
    out.writeInt(obj.getDayOfMonth)
    out.writeByte('}')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
