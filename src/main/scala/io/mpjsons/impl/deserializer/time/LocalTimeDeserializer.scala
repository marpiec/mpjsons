package io.mpjsons.impl.deserializer.time

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.deserializer.values.{ByteDeserializer, IntDeserializer}

import java.time.LocalTime

object LocalTimeDeserializer extends JsonTypeDeserializer[LocalTime] {

  override def deserialize(jsonIterator: StringIterator): LocalTime = {

    jsonIterator.consumeObjectStart()
    jsonIterator.skipWhitespaceChars()

    var hour: Byte = -1
    var minute: Byte = -1
    var second: Byte = -1
    var nano: Int = -1

    for(i <- 0 until 4) {

      if(i > 0) {
        jsonIterator.skipWhitespaceChars()
        jsonIterator.consumeArrayValuesSeparator()
      }

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.skipWhitespaceChars()
      jsonIterator.consumeFieldValueSeparator()
      identifier match {
        case "hour" => hour = ByteDeserializer.deserialize(jsonIterator)
        case "minute" => minute = ByteDeserializer.deserialize(jsonIterator)
        case "second" => second = ByteDeserializer.deserialize(jsonIterator)
        case "nano" => nano = IntDeserializer.deserialize(jsonIterator)
        case _ => throw new RuntimeException("Incorrect identifier in LocalTime '" + identifier + "'")
      }
    }

    if(hour < -1) {
      throw new RuntimeException("Cannot deserialize LocalTime due to missing 'hour' field")
    } else if(minute < -1) {
      throw new RuntimeException("Cannot deserialize LocalTime due to missing 'minute' field")
    } else if (second < -1) {
      throw new RuntimeException("Cannot deserialize LocalTime due to missing 'second' field")
    } else if (nano < -1) {
      throw new RuntimeException("Cannot deserialize LocalTime due to missing 'nano' field")
    } else {
      jsonIterator.skipWhitespaceChars()
      jsonIterator.consumeObjectEnd()
      LocalTime.of(hour, minute, second, nano)
    }

  }
}
