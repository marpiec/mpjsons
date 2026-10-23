package io.mpjsons.legacy.impl.deserializer.time

import io.mpjsons.legacy.impl.util.Messages

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.IdentifierDeserializer
import io.mpjsons.legacy.impl.deserializer.values.StringDeserializer

import java.time.{LocalDate, LocalDateTime, LocalTime}

/** Reads {"date":{...},"time":{...}}; fields may come in any order, both are required. */
object LocalDateTimeDeserializer extends JsonTypeDeserializer[LocalDateTime] {

  override def deserialize(jsonIterator: StringIterator): LocalDateTime = {

    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == '"') {
      return LocalDateTime.parse(StringDeserializer.deserialize(jsonIterator))
    }

    jsonIterator.consumeObjectStart()
    jsonIterator.skipWhitespaceChars()

    var time: LocalTime = null
    var date: LocalDate = null

    while (jsonIterator.currentChar != '}') {
      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.consumeFieldValueSeparator()
      identifier match {
        case "time" => time = LocalTimeDeserializer.deserialize(jsonIterator)
        case "date" => date = LocalDateDeserializer.deserialize(jsonIterator)
        case _ => throw new IllegalArgumentException("Incorrect identifier in LocalDateTime '" + Messages.abbreviate(identifier) + "'")
      }
      jsonIterator.afterValue('}')
    }
    jsonIterator.consumeObjectEnd()

    if (date == null) {
      throw new IllegalArgumentException("Cannot deserialize LocalDateTime due to missing 'date' field")
    } else if (time == null) {
      throw new IllegalArgumentException("Cannot deserialize LocalDateTime due to missing 'time' field")
    } else {
      LocalDateTime.of(date, time)
    }
  }
}
