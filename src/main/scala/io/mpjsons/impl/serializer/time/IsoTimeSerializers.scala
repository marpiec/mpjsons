package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer

import java.time.{Duration, LocalDate, LocalDateTime, LocalTime}

/**
 * ISO-8601 string forms of the java.time types (`MPJsonsConfig(isoTime = true)`), e.g. "2023-07-23",
 * "12:30:17.000000125", "2023-08-23T14:30:17.000000125", "PT2M3S". None of them needs JSON escaping.
 */
object IsoLocalDateSerializer extends JsonTypeSerializer[LocalDate] {
  override def serialize(obj: LocalDate, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('"').append(obj.toString).append('"')
  }
}

object IsoLocalTimeSerializer extends JsonTypeSerializer[LocalTime] {
  override def serialize(obj: LocalTime, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('"').append(obj.toString).append('"')
  }
}

object IsoLocalDateTimeSerializer extends JsonTypeSerializer[LocalDateTime] {
  override def serialize(obj: LocalDateTime, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('"').append(obj.toString).append('"')
  }
}

object IsoDurationSerializer extends JsonTypeSerializer[Duration] {
  override def serialize(obj: Duration, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append('"').append(obj.toString).append('"')
  }
}
