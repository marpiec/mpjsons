package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer

import java.time.LocalTime

object LocalTimeSerializer extends JsonTypeSerializer[LocalTime] {

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
}
