package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator


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

  /**
   * Reads a floating point literal with exactly the result of `java.lang.Double.parseDouble`.
   * Decimal literals with up to 18 significant digits are converted without creating a String
   * (Clinger's fast path, then the Eisel-Lemire algorithm); anything else is left to `parseDouble`.
   */
  def readDouble(jsonIterator: StringIterator): Double = {
    val start = scanNumber(jsonIterator)
    val end = jsonIterator.nextIndex - 1
    val input = jsonIterator.stringValue
    val value = parseExactly(input, start, end, isFloat = false)
    if (value == value) value else java.lang.Double.parseDouble(input.substring(start, end))
  }

  /** Reads a floating point literal with exactly the result of `java.lang.Float.parseFloat`. */
  def readFloat(jsonIterator: StringIterator): Float = {
    val start = scanNumber(jsonIterator)
    val end = jsonIterator.nextIndex - 1
    val input = jsonIterator.stringValue
    val value = parseExactly(input, start, end, isFloat = true)
    if (value == value) value.toFloat else java.lang.Float.parseFloat(input.substring(start, end))
  }

  private def isNumberPart(c: Char): Boolean = {
    c >= '0' && c <= '9' || c == '-' || c == '+' || c == '.' || c == 'e' || c == 'E'
  }

  /** Exact powers of 10 representable as double. */
  private val doublePowersOfTen: Array[Double] = Array.tabulate(23)(i => ("1e" + i).toDouble)
  /** Exact powers of 10 representable as float. */
  private val floatPowersOfTen: Array[Float] = Array.tabulate(11)(i => ("1e" + i).toFloat)

  private final val MaxExactDoubleMantissa = 1L << 53
  private final val MaxExactFloatMantissa = 1L << 24

  /**
   * Parses -?digits(.digits)?([eE][+-]?digits)? with at most 18 significant digits. Returns the correctly rounded
   * value, or NaN when the literal has another form or the result cannot be decided cheaply.
   * Floats use only the exact fast path (mantissa and power of ten exactly representable, one operation).
   */
  private def parseExactly(input: String, start: Int, end: Int, isFloat: Boolean): Double = {
    val NotHandled = Double.NaN
    var index = start
    val negative = index < end && input.charAt(index) == '-'
    if (negative) index += 1

    var mantissa = 0L
    var digits = 0
    var exponent = 0
    var c = '\u0000'

    val integerStart = index
    while (index < end && { c = input.charAt(index); c >= '0' && c <= '9' }) {
      if (mantissa != 0 || c != '0') {
        if (digits == 18) return NotHandled
        mantissa = mantissa * 10 + (c - '0')
        digits += 1
      }
      index += 1
    }
    if (index == integerStart) return NotHandled // e.g. ".5", rare, left for parseDouble

    if (index < end && input.charAt(index) == '.') {
      index += 1
      val fractionStart = index
      while (index < end && { c = input.charAt(index); c >= '0' && c <= '9' }) {
        if (mantissa != 0 || c != '0') {
          if (digits == 18) return NotHandled
          mantissa = mantissa * 10 + (c - '0')
          digits += 1
        }
        exponent -= 1
        index += 1
      }
      if (index == fractionStart) return NotHandled
    }

    if (index < end && (input.charAt(index) == 'e' || input.charAt(index) == 'E')) {
      index += 1
      val exponentNegative = index < end && input.charAt(index) == '-'
      if (index < end && (input.charAt(index) == '-' || input.charAt(index) == '+')) index += 1
      val exponentStart = index
      var exponentValue = 0
      while (index < end && { c = input.charAt(index); c >= '0' && c <= '9' }) {
        if (exponentValue > 100000) return NotHandled
        exponentValue = exponentValue * 10 + (c - '0')
        index += 1
      }
      if (index == exponentStart) return NotHandled
      exponent += (if (exponentNegative) -exponentValue else exponentValue)
    }

    if (index != end) {
      NotHandled
    } else if (isFloat) {
      if (mantissa > MaxExactFloatMantissa || exponent < -10 || exponent > 10) {
        NotHandled
      } else {
        val value = if (exponent >= 0) mantissa.toFloat * floatPowersOfTen(exponent) else mantissa.toFloat / floatPowersOfTen(-exponent)
        if (negative) -value else value
      }
    } else if (mantissa <= MaxExactDoubleMantissa && exponent >= -22 && exponent <= 22) {
      val value = if (exponent >= 0) mantissa.toDouble * doublePowersOfTen(exponent) else mantissa.toDouble / doublePowersOfTen(-exponent)
      if (negative) -value else value
    } else {
      val bits = EiselLemire.toDoubleBits(mantissa, exponent)
      if (bits == EiselLemire.Failure) {
        NotHandled
      } else {
        java.lang.Double.longBitsToDouble(if (negative) bits | Long.MinValue else bits)
      }
    }
  }
}

/**
 * @author Marcin Pieciukiewicz
 */
abstract class AbstractFloatingPointDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readDouble(jsonIterator: StringIterator): Double = AbstractFloatingPointDeserializer.readDouble(jsonIterator)

  protected def readFloat(jsonIterator: StringIterator): Float = AbstractFloatingPointDeserializer.readFloat(jsonIterator)

}
