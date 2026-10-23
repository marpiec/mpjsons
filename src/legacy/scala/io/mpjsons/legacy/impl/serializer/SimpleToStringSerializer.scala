package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object SimpleToStringSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append(obj)
  }
}
