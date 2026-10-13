package io.mpjsons.impl.deserializer.time

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.deserializer.values.StringDeserializer
import io.mpjsons.impl.deserializer.values.IntDeserializer

import java.time.LocalDate

/** Reads {"year":y,"month":m,"day":d}; fields may come in any order, all three are required. */
object LocalDateDeserializer extends JsonTypeDeserializer[LocalDate] {

  override def deserialize(jsonIterator: StringIterator): LocalDate = {

    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == '"') {
      return LocalDate.parse(StringDeserializer.deserialize(jsonIterator))
    }

    jsonIterator.consumeObjectStart()
    jsonIterator.skipWhitespaceChars()

    var year = 0
    var month = 0
    var day = 0
    var hasYear = false
    var hasMonth = false
    var hasDay = false

    while (jsonIterator.currentChar != '}') {
      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      jsonIterator.consumeFieldValueSeparator()
      identifier match {
        case "year" => year = IntDeserializer.deserialize(jsonIterator); hasYear = true
        case "month" => month = IntDeserializer.deserialize(jsonIterator); hasMonth = true
        case "day" => day = IntDeserializer.deserialize(jsonIterator); hasDay = true
        case _ => throw new IllegalArgumentException("Incorrect identifier in LocalDate '" + identifier + "'")
      }
      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
        jsonIterator.skipWhitespaceChars()
      }
    }
    jsonIterator.consumeObjectEnd()

    if (!hasYear) {
      throw new IllegalArgumentException("Cannot deserialize LocalDate due to missing 'year' field")
    } else if (!hasMonth) {
      throw new IllegalArgumentException("Cannot deserialize LocalDate due to missing 'month' field")
    } else if (!hasDay) {
      throw new IllegalArgumentException("Cannot deserialize LocalDate due to missing 'day' field")
    } else {
      LocalDate.of(year, month, day)
    }
  }
}
