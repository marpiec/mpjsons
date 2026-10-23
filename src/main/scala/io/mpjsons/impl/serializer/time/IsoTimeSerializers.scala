package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.serializer.{Utf8Output, Utf8Writer}

import java.time.{Duration, LocalDate, LocalDateTime, LocalTime}

/**
 * ISO-8601 string forms of the java.time types (`MPJsonsConfig(isoTime = true)`), e.g. "2023-07-23",
 * "12:30:17.000000125", "2023-08-23T14:30:17.000000125", "PT2M3S". None of them needs JSON escaping.
 */
object IsoLocalDateSerializer extends JsonTypeSerializer[LocalDate] with Utf8Writer[LocalDate] {
  override def serialize(obj: LocalDate, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('"').append(obj.toString).append('"')
  }
  override def writeUtf8(obj: LocalDate, out: Utf8Output): Unit = IsoTimeSerializers.writeQuoted(obj.toString, out)
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}

object IsoLocalTimeSerializer extends JsonTypeSerializer[LocalTime] with Utf8Writer[LocalTime] {
  override def serialize(obj: LocalTime, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('"').append(obj.toString).append('"')
  }
  override def writeUtf8(obj: LocalTime, out: Utf8Output): Unit = IsoTimeSerializers.writeQuoted(obj.toString, out)
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}

object IsoLocalDateTimeSerializer extends JsonTypeSerializer[LocalDateTime] with Utf8Writer[LocalDateTime] {
  override def serialize(obj: LocalDateTime, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('"').append(obj.toString).append('"')
  }
  override def writeUtf8(obj: LocalDateTime, out: Utf8Output): Unit = IsoTimeSerializers.writeQuoted(obj.toString, out)
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}

object IsoDurationSerializer extends JsonTypeSerializer[Duration] with Utf8Writer[Duration] {
  override def serialize(obj: Duration, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('"').append(obj.toString).append('"')
  }
  override def writeUtf8(obj: Duration, out: Utf8Output): Unit = IsoTimeSerializers.writeQuoted(obj.toString, out)
  override private[mpjsons] def writesUtf8Itself: Boolean = true
}

private object IsoTimeSerializers {
  def writeQuoted(text: String, out: Utf8Output): Unit = {
    out.writeByte('"')
    out.writeText(text)
    out.writeByte('"')
  }
}
