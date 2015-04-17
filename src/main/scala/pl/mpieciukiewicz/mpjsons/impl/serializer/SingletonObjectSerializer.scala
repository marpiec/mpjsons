package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory

class SingletonObjectSerializer() extends JsonTypeSerializer[AnyRef] {

  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{}")
  }
}
