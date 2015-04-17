package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

class SimpleToStringSerializer() extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder) {
    jsonBuilder.append(obj)
  }
}
