package io.mpjsons.impl.deserializer.time

import io.mpjsons.impl.util.Messages

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.deserializer.values.StringDeserializer
import io.mpjsons.impl.deserializer.values.{IntDeserializer, LongDeserializer}

import java.time.Duration

/** Reads {"seconds":s,"nanos":n}; fields may come in any order, both are required. */
object DurationDeserializer extends JsonTypeDeserializer[Duration] {

  override def deserialize(jsonIterator: StringIterator): Duration = {

    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == '"') {
      return Duration.parse(StringDeserializer.deserialize(jsonIterator))
    }

    jsonIterator.consumeObjectStart()
    jsonIterator.skipWhitespaceChars()

    var seconds: Long = 0
    var nanos: Int = 0
    var hasSeconds = false
    var hasNanos = false

    while (jsonIterator.currentChar != '}') {
      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.consumeFieldValueSeparator()
      identifier match {
        case "seconds" => seconds = LongDeserializer.deserialize(jsonIterator); hasSeconds = true
        case "nanos" => nanos = IntDeserializer.deserialize(jsonIterator); hasNanos = true
        case _ => throw new IllegalArgumentException("Incorrect identifier in Duration '" + Messages.abbreviate(identifier) + "'")
      }
      jsonIterator.afterValue('}')
    }
    jsonIterator.consumeObjectEnd()

    if (!hasSeconds) {
      throw new IllegalArgumentException("Cannot deserialize Duration due to missing 'seconds' field")
    } else if (!hasNanos) {
      throw new IllegalArgumentException("Cannot deserialize Duration due to missing 'nanos' field")
    } else {
      Duration.ofSeconds(seconds, nanos)
    }
  }
}
