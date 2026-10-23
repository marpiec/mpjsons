package io.mpjsons.impl.deserializer

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractStringDeserializer
import io.mpjsons.impl.deserializer.values.StringDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object IdentifierDeserializer {

  def deserialize(jsonIterator: StringIterator): String = {

    jsonIterator.skipWhitespaceChars()

    val quoted = jsonIterator.currentChar.equals('"')

    if(quoted) {
      StringDeserializer.deserialize(jsonIterator)
    } else {
      val identifier = new StringBuilder()

      identifier.append(jsonIterator.currentChar)

      jsonIterator.nextChar()

      while (jsonIterator.currentChar != ':' && (!quoted && !jsonIterator.currentChar.isWhitespace || quoted && !jsonIterator.currentChar.equals('"'))) {
        identifier.append(jsonIterator.currentChar)
        jsonIterator.nextChar()
      }

      identifier.toString()

    }

  }

  /** Same as `deserialize`, but without creating the identifier String in the common case. */
  def skip(jsonIterator: StringIterator): Unit = {
    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == '"') {
      AbstractStringDeserializer.skipString(jsonIterator)
    } else {
      skipBare(jsonIterator)
    }
  }

  /**
   * True if the identifier at the cursor equals `expected`; the cursor is left after the identifier, as after
   * `deserialize`. Avoids creating the identifier String when it is quoted and has no escape sequences.
   */
  def matches(jsonIterator: StringIterator, expected: String): Boolean = {
    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == '"') {
      val start = jsonIterator.nextIndex
      val end = jsonIterator.stringValue.indexOf('"', start)
      if (end >= 0 && jsonIterator.backslashIndexFrom(start) > end) {
        jsonIterator.seek(end + 1)
        jsonIterator.rangeEquals(start, end, expected)
      } else {
        deserialize(jsonIterator) == expected
      }
    } else {
      deserialize(jsonIterator) == expected
    }
  }

  /** Bare key: its first character and everything up to ':' or whitespace (the end of input is an error). */
  private def skipBare(jsonIterator: StringIterator): Unit = {
    val input = jsonIterator.stringValue
    val length = jsonIterator.stringLength
    var index = jsonIterator.nextIndex
    while (index < length && { val c = input.charAt(index); c != ':' && !StringIterator.isWhitespace(c) }) {
      index += 1
    }
    if (index >= length) {
      // the end marker would have been taken as a part of the identifier, and the next step fails
      jsonIterator.seek(length)
      throw new IllegalArgumentException("Unexpected end of Json")
    }
    jsonIterator.seek(index)
  }

}
