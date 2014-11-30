package pl.marpiec.mpjsons.impl.serializer

import pl.marpiec.mpjsons.JsonTypeSerializer

object SingletonObjectSerializer extends JsonTypeSerializer {

  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{}")
  }
}
