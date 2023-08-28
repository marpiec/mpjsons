package io.mpjsons.impl.deserializer.time

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.deserializer.values.{ByteDeserializer, IntDeserializer, LongDeserializer}

import java.time.Duration

object DurationDeserializer extends JsonTypeDeserializer[Duration] {

  override def deserialize(jsonIterator: StringIterator): Duration = {

    jsonIterator.consumeObjectStart()
    jsonIterator.skipWhitespaceChars()

    var seconds: Long = -1
    var nanos: Int = -1

    for(i <- 0 until 2) {

      if(i > 0) {
        jsonIterator.skipWhitespaceChars()
        jsonIterator.consumeArrayValuesSeparator()
      }

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.skipWhitespaceChars()
      jsonIterator.consumeFieldValueSeparator()
      identifier match {
        case "seconds" => seconds = LongDeserializer.deserialize(jsonIterator)
        case "nanos" => nanos = IntDeserializer.deserialize(jsonIterator)
        case _ => throw new RuntimeException("Incorrect identifier in Duration '" + identifier + "'")
      }
    }

    if(seconds < -1) {
      throw new RuntimeException("Cannot deserialize Duration due to missing 'seconds' field")
    } else if(nanos < -1) {
      throw new RuntimeException("Cannot deserialize Duration due to missing 'nanos' field")
    } else {
      jsonIterator.skipWhitespaceChars()
      jsonIterator.consumeObjectEnd()
      Duration.ofSeconds(seconds, nanos)
    }

  }
}
