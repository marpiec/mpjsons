package io.mpjsons.impl.deserializer.time

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.IdentifierDeserializer

import java.time.{LocalDate, LocalDateTime, LocalTime}

object LocalDateTimeDeserializer extends JsonTypeDeserializer[LocalDateTime] {

  override def deserialize(jsonIterator: StringIterator): LocalDateTime = {

    jsonIterator.consumeObjectStart()
    jsonIterator.skipWhitespaceChars()

    var time: LocalTime = null
    var date: LocalDate = null

    for(i <- 0 until 2) {

      if (i > 0) {
        jsonIterator.skipWhitespaceChars()
        jsonIterator.consumeArrayValuesSeparator()
      }

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.skipWhitespaceChars()
      jsonIterator.consumeFieldValueSeparator()

      identifier match {
        case "time" => time = LocalTimeDeserializer.deserialize(jsonIterator)
        case "date" => date = LocalDateDeserializer.deserialize(jsonIterator)
        case _ => throw new RuntimeException("Incorrect identifier in LocalDateTime '" + identifier + "'")
      }

      jsonIterator.skipWhitespaceChars()
    }

    if(time == null) {
      throw new RuntimeException("Cannot deserialize LocalDateTime due to missing 'time' field")
    } else if(date == null) {
      throw new RuntimeException("Cannot deserialize LocalDateTime due to missing 'date' field")
    } else {
      jsonIterator.skipWhitespaceChars()
      jsonIterator.consumeObjectEnd()
      LocalDateTime.of(date, time)
    }
  }
}
