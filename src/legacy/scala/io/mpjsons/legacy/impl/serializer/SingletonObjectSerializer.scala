package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.JsonTypeSerializer

object SingletonObjectSerializer extends JsonTypeSerializer[AnyRef] {

  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{}")
  }
}
