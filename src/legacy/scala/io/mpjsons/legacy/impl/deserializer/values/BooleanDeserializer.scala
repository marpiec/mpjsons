package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.StringIterator

/**
 * Reads the literals true / false by comparing the raw input in place.
 * @author Marcin Pieciukiewicz
 */
object BooleanDeserializer extends JsonTypeDeserializer[Boolean] {

  override def deserialize(jsonIterator: StringIterator): Boolean = {

    jsonIterator.skipWhitespaceChars()

    val input = jsonIterator.stringValue
    val start = jsonIterator.nextIndex - 1

    if (input.startsWith("true", start)) {
      jsonIterator.seek(start + 4)
      true
    } else if (input.startsWith("false", start)) {
      jsonIterator.seek(start + 5)
      false
    } else {
      val found = if (start >= jsonIterator.stringLength) "end of input" else "'" + jsonIterator.currentChar + "'"
      throw new IllegalArgumentException("Expected a boolean (true or false) but found " + found)
    }
  }

}

object JavaBooleanDeserializer extends JsonTypeDeserializer[java.lang.Boolean] {

  override def deserialize(jsonIterator: StringIterator): java.lang.Boolean = {
    if (BooleanDeserializer.deserialize(jsonIterator)) java.lang.Boolean.TRUE else java.lang.Boolean.FALSE
  }

}
