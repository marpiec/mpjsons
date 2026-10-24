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
   * Decodes into a buffer owned by the iterator, so only the resulting String is allocated. The next quote and the
   * next backslash are found with `indexOf` (vectorized) and the runs between them are copied in bulk; the input is
   * still processed in order, so errors are the same as with a char-by-char scan.
   */
  private def readStringWithEscapes(jsonIterator: StringIterator, input: String, start: Int, escapeIndex: Int): String = {
    val length = input.length
    var buffer = jsonIterator.charBuffer(escapeIndex - start + 32, 0)
    input.getChars(start, escapeIndex, buffer, 0)
    var size = escapeIndex - start

    var index = escapeIndex
    // next '"' at or after `index` (may be an escaped one), `length` if there is none; searched again only once passed
    var quoteIndex = -1
    while (true) {
      if (quoteIndex < index) {
        val found = input.indexOf('"', index)
        quoteIndex = if (found < 0) length else found
      }
      val special = Math.min(quoteIndex, jsonIterator.backslashIndexFrom(index))
      // copy the run of ordinary characters, leaving room for one decoded character
      buffer = jsonIterator.charBuffer(size + (special - index) + 2, size)
      input.getChars(index, special, buffer, size)
      size += special - index
      if (special >= length) {
        throw new IllegalArgumentException("Unexpected end of Json")
      }
      if (special == quoteIndex) {
        jsonIterator.seek(special + 1)
        return new String(buffer, 0, size)
      }
      index = special + 1
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
          val hex = hexValue(input, index + 1)
          // anything but four hex digits (a sign, other Unicode digits) goes through parseInt as before: same value or error
          val codePoint = if (hex >= 0) hex else Integer.parseInt(input.substring(index + 1, index + 5), 16)
          size += Character.toChars(codePoint, buffer, size)
          index += 4
        case other => throw new IllegalArgumentException("Unsupported control character [\\" + other + "]")
      }
      index += 1
    }
    throw new IllegalStateException("unreachable")
  }

  /** The value of the four hex digits at `from`, or -1 if they are not four ASCII hex digits. */
  private def hexValue(input: String, from: Int): Int = {
    var value = 0
    var i = from
    while (i < from + 4) {
      val c = input.charAt(i)
      val digit =
        if (c >= '0' && c <= '9') c - '0'
        else if (c >= 'a' && c <= 'f') c - 'a' + 10
        else if (c >= 'A' && c <= 'F') c - 'A' + 10
        else return -1
      value = (value << 4) | digit
      i += 1
    }
    value
  }
}

abstract class AbstractStringDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readString(jsonIterator: StringIterator): String = {
    AbstractStringDeserializer.readString(jsonIterator)
  }
}
