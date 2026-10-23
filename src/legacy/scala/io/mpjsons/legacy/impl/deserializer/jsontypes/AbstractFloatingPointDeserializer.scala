package io.mpjsons.legacy.impl.deserializer.jsontypes

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.StringIterator


object AbstractFloatingPointDeserializer {

  /**
   * Scans a number literal (digits and any of "-+.eE") at the current position (leading whitespace allowed),
   * leaves the cursor on the first character after it and returns the index where the literal started.
   * Throws IllegalArgumentException when no number character is found.
   */
  def scanNumber(jsonIterator: StringIterator): Int = {

    jsonIterator.skipWhitespaceChars()

    val input = jsonIterator.stringValue
    val length = jsonIterator.stringLength
    val start = jsonIterator.nextIndex - 1 // index of currentChar
    var index = start

    while (index < length && isNumberPart(input.charAt(index))) {
      index += 1
    }

    if (index == start) {
      throw new IllegalArgumentException("Expected a number but found " + AbstractIntegerDeserializer.describeCurrent(jsonIterator))
    }

    jsonIterator.seek(index)
    start
  }

  /** Skips a number literal without materialising it. */
  def skipNumber(jsonIterator: StringIterator): Unit = {
    scanNumber(jsonIterator)
  }

  /** Reads a floating point literal; plain integers take an allocation-free path. */
  def readDouble(jsonIterator: StringIterator): Double = {
    val start = scanNumber(jsonIterator)
    val end = jsonIterator.nextIndex - 1
    val input = jsonIterator.stringValue
    if (isPlainInteger(input, start, end)) {
      // fix applied to the reference copy: "-0" is -0.0, as Double.parseDouble gives (0.7.2 returned +0.0)
      { val v = parsePlainInteger(input, start, end).toDouble; if (v == 0 && input.charAt(start) == '-') -0.0 else v }
    } else {
      java.lang.Double.parseDouble(input.substring(start, end))
    }
  }

  def readFloat(jsonIterator: StringIterator): Float = {
    val start = scanNumber(jsonIterator)
    val end = jsonIterator.nextIndex - 1
    val input = jsonIterator.stringValue
    if (isPlainInteger(input, start, end)) {
      { val v = parsePlainInteger(input, start, end).toFloat; if (v == 0 && input.charAt(start) == '-') -0.0f else v }
    } else {
      java.lang.Float.parseFloat(input.substring(start, end))
    }
  }

  private def isNumberPart(c: Char): Boolean = {
    c >= '0' && c <= '9' || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E'
  }

  /** An optional '-' followed by 1 to 18 digits: exactly representable while accumulating in a Long. */
  private def isPlainInteger(input: String, start: Int, end: Int): Boolean = {
    var index = start
    if (index < end && input.charAt(index) == '-') {
      index += 1
    }
    val digits = end - index
    if (digits < 1 || digits > 18) {
      false
    } else {
      while (index < end) {
        val c = input.charAt(index)
        if (c < '0' || c > '9') {
          return false
        }
        index += 1
      }
      true
    }
  }

  private def parsePlainInteger(input: String, start: Int, end: Int): Long = {
    var index = start
    val negative = input.charAt(index) == '-'
    if (negative) {
      index += 1
    }
    var value = 0L
    while (index < end) {
      value = value * 10 + (input.charAt(index) - '0')
      index += 1
    }
    if (negative) -value else value
  }
}

/**
 * @author Marcin Pieciukiewicz
 */
abstract class AbstractFloatingPointDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readDouble(jsonIterator: StringIterator): Double = AbstractFloatingPointDeserializer.readDouble(jsonIterator)

  protected def readFloat(jsonIterator: StringIterator): Float = AbstractFloatingPointDeserializer.readFloat(jsonIterator)

}
