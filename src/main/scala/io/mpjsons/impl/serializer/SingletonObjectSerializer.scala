package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

class SingletonObjectSerializer() extends JsonTypeSerializer[AnyRef] {

  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{}")
  }
}
