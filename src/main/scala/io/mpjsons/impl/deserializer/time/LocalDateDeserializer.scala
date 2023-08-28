package io.mpjsons.impl.deserializer.time

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.deserializer.values.{IntDeserializer, ShortDeserializer}

import java.time.LocalDate

object LocalDateDeserializer extends JsonTypeDeserializer[LocalDate] {

  override def deserialize(jsonIterator: StringIterator): LocalDate = {

    jsonIterator.consumeObjectStart()
    jsonIterator.skipWhitespaceChars()

    var year: Int = -1
    var month: Short = -1
    var day: Short = -1

    for(i <- 0 until 3) {

      if (i > 0) {
        jsonIterator.skipWhitespaceChars()
        jsonIterator.consumeArrayValuesSeparator()
      }

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.skipWhitespaceChars()
      jsonIterator.consumeFieldValueSeparator()

      identifier match {
        case "year" => year = IntDeserializer.deserialize(jsonIterator)
        case "month" => month = ShortDeserializer.deserialize(jsonIterator)
        case "day" => day = ShortDeserializer.deserialize(jsonIterator)
        case _ => throw new RuntimeException("Incorrect identifier in LocalDate '" + identifier + "'")
      }
    }

    if(year < -1) {
      throw new RuntimeException("Cannot deserialize LocalDate due to missing 'year' field")
    } else if(month < -1) {
      throw new RuntimeException("Cannot deserialize LocalDate due to missing 'month' field")
    } else if(day < -1) {
      throw new RuntimeException("Cannot deserialize LocalDate due to missing 'day' field")
    } else {
      jsonIterator.skipWhitespaceChars()
      jsonIterator.consumeObjectEnd()
      LocalDate.of(year, month, day)
    }

  }
}
