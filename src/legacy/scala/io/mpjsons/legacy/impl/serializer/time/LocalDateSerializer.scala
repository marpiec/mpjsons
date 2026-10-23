package io.mpjsons.legacy.impl.serializer.time

import io.mpjsons.legacy.JsonTypeSerializer

import java.time.LocalDate

object LocalDateSerializer extends JsonTypeSerializer[LocalDate] {

  override def serialize(obj: LocalDate, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{\"year\":")
    jsonBuilder.append(obj.getYear)
    jsonBuilder.append(",\"month\":")
    jsonBuilder.append(obj.getMonthValue)
    jsonBuilder.append(",\"day\":")
    jsonBuilder.append(obj.getDayOfMonth)
    jsonBuilder.append('}')
  }
}
