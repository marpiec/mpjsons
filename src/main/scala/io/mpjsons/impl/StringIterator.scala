package io.mpjsons.impl

/**
 * Special container for String that allows easy iteration through String characters.
 *
 * Invariant: `currentChar` is the character at index `nextIndex - 1`, or the end marker `'\u0000'`
 * once the cursor has moved past the last character (`nextIndex > stringLength`). Moving past the end marker throws.
 *
 * @param strict reject missing and trailing commas between members and elements (see `afterValue`); the
 *               historical, lenient behaviour is the default
 * @author Marcin Pieciukiewicz
 */
class StringIterator(val stringValue: String, val strict: Boolean) {

  def this(stringValue: String) = this(stringValue, false)

  val stringLength = stringValue.length
  var nextIndex = 1
  var currentChar: Char = if (stringLength == 0) '\u0000' else stringValue.charAt(0)

  /** Current nesting level, counted only when `MPJsonsConfig.maxDepth` is set (see DepthLimit). */
  var depth: Int = 0

  /** Enters one nesting level; fails when that would exceed `maxDepth`. */
  def enterNested(maxDepth: Int): Unit = {
    if (depth >= maxDepth) {
      throw new IllegalArgumentException("JSON nesting is deeper than maxDepth = " + maxDepth)
    }
    depth += 1
  }

  def exitNested(): Unit = {
    depth -= 1
  }

  def skipWhitespaceChars(): Unit = {
    while (StringIterator.isWhitespace(currentChar)) {
      nextChar()
    }
  }

  /** Position from which `backslashIndex` was searched, -1 if not searched yet. */
  private var backslashSearchFrom = -1
  /** Index of the first backslash at or after `backslashSearchFrom`, or `stringLength` if there is none. */
  private var backslashIndex = 0
  /** Buffer reused for decoding string literals with escape sequences. */
  private var charBuffer: Array[Char] = null

  /**
   * Index of the first backslash at or after `index`, or `stringLength` if there is none. The result is cached,
   * so scanning a document with few backslashes costs one pass over the input in total.
   */
  final def backslashIndexFrom(index: Int): Int = {
    if (backslashSearchFrom < 0 || backslashSearchFrom > index || backslashIndex < index) {
      val found = stringValue.indexOf('\\', index)
      backslashIndex = if (found < 0) stringLength else found
      backslashSearchFrom = index
    }
    backslashIndex
  }

  /** Reusable buffer of at least `minSize` chars, with the first `keep` chars of the previous buffer preserved. */
  final def charBuffer(minSize: Int, keep: Int): Array[Char] = {
    if (charBuffer == null || charBuffer.length < minSize) {
      val newBuffer = new Array[Char](if (charBuffer == null) minSize else Math.max(minSize, charBuffer.length * 2))
      if (keep > 0) System.arraycopy(charBuffer, 0, newBuffer, 0, keep)
      charBuffer = newBuffer
    }
    charBuffer
  }

  // Scratch arrays for primitive arrays of unknown length: filled while reading, then copied once at the exact size.
  // Owned by this iterator (one parse, one thread). A reader must copy its result out before anything that could read
  // another array of the same element type from this iterator runs.
  private var intScratch: Array[Int] = null
  private var longScratch: Array[Long] = null
  private var doubleScratch: Array[Double] = null

  /** Scratch Int array of at least `minSize` elements, with the first `keep` elements preserved. */
  final def intScratch(minSize: Int, keep: Int): Array[Int] = {
    if (intScratch == null || intScratch.length < minSize) {
      intScratch = if (intScratch == null) new Array[Int](minSize) else java.util.Arrays.copyOf(intScratch, Math.max(minSize, keep * 2))
    }
    intScratch
  }

  /** Scratch Long array of at least `minSize` elements, with the first `keep` elements preserved. */
  final def longScratch(minSize: Int, keep: Int): Array[Long] = {
    if (longScratch == null || longScratch.length < minSize) {
      longScratch = if (longScratch == null) new Array[Long](minSize) else java.util.Arrays.copyOf(longScratch, Math.max(minSize, keep * 2))
    }
    longScratch
  }

  /** Scratch Double array of at least `minSize` elements, with the first `keep` elements preserved. */
  final def doubleScratch(minSize: Int, keep: Int): Array[Double] = {
    if (doubleScratch == null || doubleScratch.length < minSize) {
      doubleScratch = if (doubleScratch == null) new Array[Double](minSize) else java.util.Arrays.copyOf(doubleScratch, Math.max(minSize, keep * 2))
    }
    doubleScratch
  }

  /** True if characters from `start` (inclusive) to `end` (exclusive) are equal to `text`. */
  final def rangeEquals(start: Int, end: Int, text: String): Boolean = {
    end - start == text.length && stringValue.regionMatches(start, text, 0, text.length)
  }

  def nextChar(): Unit = {
    currentChar = if (nextIndex < stringLength) {
      stringValue.charAt(nextIndex)
    } else if (nextIndex == stringLength) {
      '\u0000'
    } else {
      throw new IllegalArgumentException("Unexpected end of Json")
    }
    nextIndex = nextIndex + 1
  }

  /**
   * Positions the cursor so that `currentChar` is the character at `index`
   * (or the end marker when `index == stringLength`). Used by readers that scan ahead on the raw string.
   */
  def seek(index: Int): Unit = {
    currentChar = if (index < stringLength) {
      stringValue.charAt(index)
    } else if (index == stringLength) {
      '\u0000'
    } else {
      throw new IllegalArgumentException("Unexpected end of Json")
    }
    nextIndex = index + 1
  }

  /**
   * If the next token (after whitespace) is the literal `null`, consumes it and returns true;
   * otherwise leaves the cursor on the first non-whitespace character and returns false.
   */
  def tryConsumeNull(): Boolean = {
    skipWhitespaceChars()
    if (currentChar == 'n') {
      val start = nextIndex - 1
      if (stringValue.startsWith("null", start)) {
        seek(start + 4)
        true
      } else {
        throw new IllegalArgumentException("Unexpected token starting with 'n', expected null")
      }
    } else {
      false
    }
  }

  /**
   * Called after a member or element inside an object or array whose closing character is `closing`:
   * skips whitespace and a separating comma. In strict mode a missing comma before the next element and a
   * comma directly before `closing` are errors; otherwise both are tolerated.
   */
  def afterValue(closing: Char): Unit = {
    skipWhitespaceChars()
    if (currentChar == ',') {
      nextChar()
      skipWhitespaceChars()
      if (strict && currentChar == closing) {
        throw new IllegalArgumentException("Trailing ',' before '" + closing + "'")
      }
    } else if (strict && currentChar != closing) {
      throw new IllegalArgumentException("Expected ',' or '" + closing + "' but found " + describeCurrent)
    }
  }

  /** True once the cursor has moved past the last character. */
  def isAtEnd: Boolean = nextIndex > stringLength

  /** In strict mode, fails unless only whitespace remains after the value just read. */
  def expectEnd(): Unit = {
    if (strict) {
      skipWhitespaceChars()
      if (!isAtEnd) {
        throw new IllegalArgumentException("Unexpected content after the JSON value: " + describeCurrent)
      }
    }
  }

  def nextNonWhitespaceChar(): Unit = {
    do {
      nextChar()
    } while (currentChar.isWhitespace)
  }

  /** Steps past the current character; kept for compatibility, now always advances (to the end marker at the end). */
  def nextCharOrNullIfLast: Unit = {
    nextChar()
  }

  def isCurrentCharASmallLetter: Boolean = {
    currentChar >= 'a' && currentChar <= 'z'
  }

  def isCurrentCharADigitPart: Boolean = {
    currentChar >= '0' && currentChar <= '9' || currentChar == '-'
  }

  def isCurrentCharAFloatingPointPart: Boolean = {
    currentChar >= '0' && currentChar <= '9' || currentChar == '-' || currentChar == '+' || currentChar == '.' || currentChar == 'e' || currentChar == 'E'
  }

  def checkFutureChar: Char = stringValue.charAt(nextIndex)

  def hasNextChar: Boolean = nextIndex < stringLength

  def consumeObjectStart(): Unit = {
    skipWhitespaceChars()
    if (currentChar != '{') {
      throw new IllegalArgumentException("Object should start with '{' symbol but found " + describeCurrent)
    }
    nextChar()
  }

  def consumeObjectEnd(): Unit = {
    skipWhitespaceChars()
    if (currentChar != '}') {
      throw new IllegalArgumentException("Object should end with '}' symbol but found " + describeCurrent)
    }
    nextChar()
  }

  def consumeFieldValueSeparator(): Unit = {
    skipWhitespaceChars()
    if (currentChar != ':') {
      throw new IllegalArgumentException("Field name and value should be separated by ':' symbol but found " + describeCurrent)
    }
    nextChar()
  }

  def consumeArrayStart(): Unit = {
    skipWhitespaceChars()
    if (currentChar != '[') {
      throw new IllegalArgumentException("Array should start with '[' symbol but found " + describeCurrent)
    }
    nextChar()
  }

  def consumeArrayEnd(): Unit = {
    skipWhitespaceChars()
    if (currentChar != ']') {
      throw new IllegalArgumentException("Array should end with ']' symbol but found " + describeCurrent)
    }
    nextChar()
  }

  def consumeArrayValuesSeparator(): Unit = {
    skipWhitespaceChars()
    if (currentChar != ',') {
      throw new IllegalArgumentException("Array values should be separated by ',' symbol but found " + describeCurrent)
    }
    nextChar()
  }

  /** The current character quoted, or "end of input" once the cursor is past the last character. */
  def describeCurrent: String = if (nextIndex > stringLength) "end of input" else "'" + currentChar + "'"

  def debugGetRemainingString: String = {
    stringValue.substring(Math.min(nextIndex - 1, stringLength))
  }

  def debugGetConsumedString: String = {
    stringValue.substring(0, Math.min(nextIndex - 1, stringLength))
  }

}

object StringIterator {

  /** Same as Character.isWhitespace, with a fast path for ASCII characters. */
  @inline final def isWhitespace(c: Char): Boolean = {
    if (c > ' ') {
      c >= '\u0080' && Character.isWhitespace(c)
    } else {
      c == ' ' || c == '\n' || c == '\t' || c == '\r' || Character.isWhitespace(c)
    }
  }
}
