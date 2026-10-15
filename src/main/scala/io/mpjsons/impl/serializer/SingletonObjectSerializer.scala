package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

object SingletonObjectSerializer extends JsonTypeSerializer[AnyRef] {

  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{}")
  }
}
