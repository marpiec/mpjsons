package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator

/**
 * @author Marcin Pieciukiewicz
 */

object AbstractStringDeserializer {

  /**
   * Reads a JSON string literal starting at the current position (leading whitespace allowed) and leaves
   * the cursor on the character after the closing quote.
   * Fast path: a literal without escape sequences is returned as one substring with no intermediate buffer.
   */
  def readString(jsonIterator: StringIterator): String = {

    jsonIterator.skipWhitespaceChars()

    if (jsonIterator.currentChar != '"') {
      throw new IllegalArgumentException("String value should start with '\"', but was [" + jsonIterator.currentChar + "]")
    }

    val input = jsonIterator.stringValue
    val length = jsonIterator.stringLength
    val start = jsonIterator.nextIndex // first character after the opening quote

    var index = start
    while (index < length) {
      val currentChar = input.charAt(index)
      if (currentChar == '"') {
        jsonIterator.seek(index + 1)
        return input.substring(start, index)
      } else if (currentChar == '\\') {
        return readStringWithEscapes(jsonIterator, input, start, index)
      }
      index += 1
    }

    throw new IllegalArgumentException("Unexpected end of Json")
  }

  /** Slow path, entered at the first backslash; `escapeIndex` points at that backslash. */
  private def readStringWithEscapes(jsonIterator: StringIterator, input: String, start: Int, escapeIndex: Int): String = {
    val length = input.length
    val builder = new java.lang.StringBuilder(escapeIndex - start + 16)
    builder.append(input, start, escapeIndex)

    var runStart = escapeIndex
    var index = escapeIndex
    while (index < length) {
      val currentChar = input.charAt(index)
      if (currentChar == '"') {
        builder.append(input, runStart, index)
        jsonIterator.seek(index + 1)
        return builder.toString
      } else if (currentChar == '\\') {
        builder.append(input, runStart, index)
        index += 1
        if (index >= length) {
          throw new IllegalArgumentException("Unexpected end of Json")
        }
        input.charAt(index) match {
          case '"' => builder.append('"')
          case '\\' => builder.append('\\')
          case '/' => builder.append('/')
          case 'b' => builder.append('\b')
          case 'f' => builder.append('\f')
          case 'n' => builder.append('\n')
          case 'r' => builder.append('\r')
          case 't' => builder.append('\t')
          case 'u' =>
            if (index + 4 >= length) {
              throw new IllegalArgumentException("Unexpected end of Json")
            }
            val codePoint = Integer.parseInt(input.substring(index + 1, index + 5), 16)
            builder.appendCodePoint(codePoint)
            index += 4
          case other => throw new IllegalArgumentException("Unsupported control character [\\" + other + "]")
        }
        index += 1
        runStart = index
      } else {
        index += 1
      }
    }

    throw new IllegalArgumentException("Unexpected end of Json")
  }
}

abstract class AbstractStringDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readString(jsonIterator: StringIterator): String = {
    AbstractStringDeserializer.readString(jsonIterator)
  }
}
