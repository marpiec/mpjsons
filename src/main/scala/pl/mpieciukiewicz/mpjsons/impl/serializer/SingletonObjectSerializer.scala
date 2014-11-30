package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer

object SingletonObjectSerializer extends JsonTypeSerializer {

  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{}")
  }
}
