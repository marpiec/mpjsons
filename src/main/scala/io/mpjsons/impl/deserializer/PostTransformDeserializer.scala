package io.mpjsons.impl.deserializer

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator


class PostTransformDeserializer[T](internalDeserializer: JsonTypeDeserializer[_ <: Any], transform: T => T) extends JsonTypeDeserializer[T]{

  override def deserialize(jsonIterator: StringIterator): T = {
    transform(internalDeserializer.deserialize(jsonIterator).asInstanceOf[T])
  }
}
