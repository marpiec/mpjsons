package io.mpjsons.impl.deserializer.time

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.deserializer.values.IntDeserializer

import java.time.LocalTime

/** Reads {"hour":h,"minute":m,"second":s,"nano":n}; fields may come in any order, all four are required. */
object LocalTimeDeserializer extends JsonTypeDeserializer[LocalTime] {

  override def deserialize(jsonIterator: StringIterator): LocalTime = {

    jsonIterator.consumeObjectStart()
    jsonIterator.skipWhitespaceChars()

    var hour = 0
    var minute = 0
    var second = 0
    var nano = 0
    var hasHour = false
    var hasMinute = false
    var hasSecond = false
    var hasNano = false

    while (jsonIterator.currentChar != '}') {
      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.consumeFieldValueSeparator()
      identifier match {
        case "hour" => hour = IntDeserializer.deserialize(jsonIterator); hasHour = true
        case "minute" => minute = IntDeserializer.deserialize(jsonIterator); hasMinute = true
        case "second" => second = IntDeserializer.deserialize(jsonIterator); hasSecond = true
        case "nano" => nano = IntDeserializer.deserialize(jsonIterator); hasNano = true
        case _ => throw new IllegalArgumentException("Incorrect identifier in LocalTime '" + identifier + "'")
      }
      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
        jsonIterator.skipWhitespaceChars()
      }
    }
    jsonIterator.consumeObjectEnd()

    if (!hasHour) {
      throw new IllegalArgumentException("Cannot deserialize LocalTime due to missing 'hour' field")
    } else if (!hasMinute) {
      throw new IllegalArgumentException("Cannot deserialize LocalTime due to missing 'minute' field")
    } else if (!hasSecond) {
      throw new IllegalArgumentException("Cannot deserialize LocalTime due to missing 'second' field")
    } else if (!hasNano) {
      throw new IllegalArgumentException("Cannot deserialize LocalTime due to missing 'nano' field")
    } else {
      LocalTime.of(hour, minute, second, nano)
    }
  }
}
