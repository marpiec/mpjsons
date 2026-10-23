package io.mpjsons.legacy.impl.deserializer.jsontypes

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.StringIterator

/**
 * Allocation-free reader for JSON integer literals (an optional '-' followed by digits).
 * @author Marcin Pieciukiewicz
 */
object AbstractIntegerDeserializer {

  /**
   * Reads an integer literal at the current position (leading whitespace allowed) and leaves the cursor
   * on the first character after it. Throws NumberFormatException when the value does not fit in a Long,
   * and IllegalArgumentException when no digit is found (e.g. a string or boolean where a number was expected).
   */
  def readLong(jsonIterator: StringIterator): Long = {

    jsonIterator.skipWhitespaceChars()

    val input = jsonIterator.stringValue
    val length = jsonIterator.stringLength
    val start = jsonIterator.nextIndex - 1 // index of currentChar
    var index = start

    val negative = index < length && input.charAt(index) == '-'
    if (negative) {
      index += 1
    }

    if (index >= length || !isDigit(input.charAt(index))) {
      throw new IllegalArgumentException("Expected a number but found " + describeCurrent(jsonIterator))
    }

    // Accumulate as a negative number so that Long.MinValue is representable.
    var value = 0L
    while (index < length && isDigit(input.charAt(index))) {
      val digit = input.charAt(index) - '0'
      if (value < -922337203685477580L || (value == -922337203685477580L && digit > 8)) {
        throw overflow(input, start, index)
      }
      value = value * 10 - digit
      index += 1
    }

    jsonIterator.seek(index)

    if (negative) {
      value
    } else if (value == Long.MinValue) {
      throw overflow(input, start, index)
    } else {
      -value
    }
  }

  /** Reads an integer literal that must fit in [min, max]. */
  def readLongInRange(jsonIterator: StringIterator, min: Long, max: Long, typeName: String): Long = {
    val start = jsonIterator.nextIndex - 1
    val value = readLong(jsonIterator)
    if (value < min || value > max) {
      val end = jsonIterator.nextIndex - 1
      throw new NumberFormatException("Value out of range for " + typeName + ": \"" + jsonIterator.stringValue.substring(start, end).trim + "\"")
    }
    value
  }

  private def isDigit(c: Char): Boolean = c >= '0' && c <= '9'

  private def overflow(input: String, start: Int, index: Int): NumberFormatException = {
    var end = index
    while (end < input.length && isDigit(input.charAt(end))) {
      end += 1
    }
    new NumberFormatException("For input string: \"" + io.mpjsons.legacy.impl.util.Messages.abbreviate(input.substring(start, end)) + "\"")
  }

  private[jsontypes] def describeCurrent(jsonIterator: StringIterator): String = {
    val c = jsonIterator.currentChar
    if (c == '\u0000' && jsonIterator.nextIndex > jsonIterator.stringLength) "end of input" else "'" + c + "'"
  }
}

abstract class AbstractIntegerDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readLong(jsonIterator: StringIterator): Long = AbstractIntegerDeserializer.readLong(jsonIterator)

  protected def readInt(jsonIterator: StringIterator): Int =
    AbstractIntegerDeserializer.readLongInRange(jsonIterator, Int.MinValue, Int.MaxValue, "Int").toInt

  protected def readShort(jsonIterator: StringIterator): Short =
    AbstractIntegerDeserializer.readLongInRange(jsonIterator, Short.MinValue, Short.MaxValue, "Short").toShort

  protected def readByte(jsonIterator: StringIterator): Byte =
    AbstractIntegerDeserializer.readLongInRange(jsonIterator, Byte.MinValue, Byte.MaxValue, "Byte").toByte

}
