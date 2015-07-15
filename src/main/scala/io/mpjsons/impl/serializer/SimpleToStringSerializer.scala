package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

class SimpleToStringSerializer() extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder) {
    jsonBuilder.append(obj)
  }
}
