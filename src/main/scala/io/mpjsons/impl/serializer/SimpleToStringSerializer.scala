package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object SimpleToStringSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append(obj)
  }
}
