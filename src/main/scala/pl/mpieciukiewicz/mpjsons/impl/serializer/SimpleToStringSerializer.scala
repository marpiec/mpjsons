package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object SimpleToStringSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder)(implicit serializerFactory: SerializerFactory) {
    jsonBuilder.append(obj)
  }
}
