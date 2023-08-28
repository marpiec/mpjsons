package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer

import java.time.LocalDateTime

object LocalDateTimeSerializer extends JsonTypeSerializer[LocalDateTime] {

  override def serialize(obj: LocalDateTime, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{\"date\":")
    LocalDateSerializer.serialize(obj.toLocalDate, jsonBuilder)
    jsonBuilder.append(",\"time\":")
    LocalTimeSerializer.serialize(obj.toLocalTime, jsonBuilder)
    jsonBuilder.append('}')
  }
}
