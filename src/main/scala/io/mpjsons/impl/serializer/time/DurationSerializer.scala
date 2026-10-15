package io.mpjsons.impl.serializer.time

import io.mpjsons.JsonTypeSerializer

import java.time.Duration

object DurationSerializer extends JsonTypeSerializer[Duration] {

  override def serialize(obj: Duration, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{\"seconds\":")
    jsonBuilder.append(obj.getSeconds)
    jsonBuilder.append(",\"nanos\":")
    jsonBuilder.append(obj.getNano)
    jsonBuilder.append('}')
  }
}
