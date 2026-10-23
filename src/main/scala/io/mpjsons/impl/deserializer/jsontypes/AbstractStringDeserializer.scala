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
   * Fast path: a literal without escape sequences is found with `String.indexOf` (vectorized by the JVM) and
   * returned as one substring with no intermediate buffer.
   */
  def readString(jsonIterator: StringIterator): String = {
    val start = openingQuote(jsonIterator)
    val input = jsonIterator.stringValue
    val end = input.indexOf('"', start)
    if (end >= 0 && jsonIterator.backslashIndexFrom(start) > end) {
      jsonIterator.seek(end + 1)
      input.substring(start, end)
    } else if (end < 0 && jsonIterator.backslashIndexFrom(start) >= jsonIterator.stringLength) {
      throw new IllegalArgumentException("Unexpected end of Json")
    } else {
      readStringWithEscapes(jsonIterator, input, start, jsonIterator.backslashIndexFrom(start))
    }
  }

  /** Skips a JSON string literal, validating it exactly like `readString`, without creating the String in the common case. */
  def skipString(jsonIterator: StringIterator): Unit = {
    val start = openingQuote(jsonIterator)
    val end = jsonIterator.stringValue.indexOf('"', start)
    if (end >= 0 && jsonIterator.backslashIndexFrom(start) > end) {
      jsonIterator.seek(end + 1)
    } else {
      jsonIterator.seek(start - 1)
      readString(jsonIterator)
    }
  }

  /** Checks the opening quote (leading whitespace allowed) and returns the index of the first character after it. */
  private def openingQuote(jsonIterator: StringIterator): Int = {
    jsonIterator.skipWhitespaceChars()

    if (jsonIterator.currentChar != '"') {
      throw new IllegalArgumentException("String value should start with '\"', but was [" + jsonIterator.currentChar + "]")
    }
    jsonIterator.nextIndex
  }

  /**
   * Slow path, entered at the first backslash; `escapeIndex` points at that backslash.
   * Decodes into a buffer owned by the iterator, so only the resulting String is allocated.
   */
  private def readStringWithEscapes(jsonIterator: StringIterator, input: String, start: Int, escapeIndex: Int): String = {
    val length = input.length
    var buffer = jsonIterator.charBuffer(escapeIndex - start + 32, 0)
    input.getChars(start, escapeIndex, buffer, 0)
    var size = escapeIndex - start

    var runStart = escapeIndex
    var index = escapeIndex
    while (index < length) {
      val currentChar = input.charAt(index)
      if (currentChar == '"' || currentChar == '\\') {
        // copy the run of ordinary characters, leaving room for one decoded character
        buffer = jsonIterator.charBuffer(size + (index - runStart) + 2, size)
        input.getChars(runStart, index, buffer, size)
        size += index - runStart
      }
      if (currentChar == '"') {
        jsonIterator.seek(index + 1)
        return new String(buffer, 0, size)
      } else if (currentChar == '\\') {
        index += 1
        if (index >= length) {
          throw new IllegalArgumentException("Unexpected end of Json")
        }
        input.charAt(index) match {
          case '"' => buffer(size) = '"'; size += 1
          case '\\' => buffer(size) = '\\'; size += 1
          case '/' => buffer(size) = '/'; size += 1
          case 'b' => buffer(size) = '\b'; size += 1
          case 'f' => buffer(size) = '\f'; size += 1
          case 'n' => buffer(size) = '\n'; size += 1
          case 'r' => buffer(size) = '\r'; size += 1
          case 't' => buffer(size) = '\t'; size += 1
          case 'u' =>
            if (index + 4 >= length) {
              throw new IllegalArgumentException("Unexpected end of Json")
            }
            val codePoint = Integer.parseInt(input.substring(index + 1, index + 5), 16)
            size += Character.toChars(codePoint, buffer, size)
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
