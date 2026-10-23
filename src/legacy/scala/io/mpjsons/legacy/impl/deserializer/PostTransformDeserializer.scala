package io.mpjsons.legacy.impl.deserializer

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.StringIterator


class PostTransformDeserializer[T](internalDeserializer: JsonTypeDeserializer[_ <: Any], transform: T => T) extends JsonTypeDeserializer[T]{

  override def deserialize(jsonIterator: StringIterator): T = {
    transform(internalDeserializer.deserialize(jsonIterator).asInstanceOf[T])
  }
}
