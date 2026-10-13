package io.mpjsons.impl

/**
 * Special container for String that allows easy iteration through String characters.
 *
 * Invariant: `currentChar` is the character at index `nextIndex - 1`, or the end marker `'\u0000'`
 * once the cursor has moved past the last character. Moving past the end marker throws.
 * @author Marcin Pieciukiewicz
 */
class StringIterator(val stringValue: String) {

  val stringLength = stringValue.length
  var nextIndex = 1
  var currentChar: Char = if (stringLength == 0) '\u0000' else stringValue.charAt(0)

  def skipWhitespaceChars(): Unit = {
    while (currentChar.isWhitespace) {
      nextChar()
    }
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

  def nextNonWhitespaceChar(): Unit = {
    do {
      nextChar()
    } while (currentChar.isWhitespace)
  }

  def nextCharOrNullIfLast = {
    if (nextIndex < stringValue.length()) {
      nextChar()
    } else {
      null
    }
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
      throw new IllegalArgumentException("Object should start with '{' symbol but was '" + currentChar + "'\n" + stringValue + "\n" + debugGetRemainingString)
    }
    nextChar()
  }

  def consumeObjectEnd(): Unit = {
    skipWhitespaceChars()
    if (currentChar != '}') {
      throw new IllegalArgumentException("Object should end with '{' symbol but was '" + currentChar + "'\n" + stringValue + "\n" + debugGetRemainingString)
    }
    nextChar()
  }

  def consumeFieldValueSeparator(): Unit = {
    skipWhitespaceChars()
    if (currentChar != ':') {
      throw new IllegalArgumentException("Field name and value should be separated by ':' symbol but was '" + currentChar + "'\n" + stringValue + "\n" + debugGetRemainingString)
    }
    nextChar()
  }

  def consumeArrayStart(): Unit = {
    skipWhitespaceChars()
    if (currentChar != '[') {
      throw new IllegalArgumentException("Array should start with '[' symbol but was '" + currentChar + "'\n" + stringValue + "\n" + debugGetRemainingString)
    }
    nextChar()
  }

  def consumeArrayEnd(): Unit = {
    skipWhitespaceChars()
    if (currentChar != ']') {
      throw new IllegalArgumentException("Array should end with ']' symbol but was '" + currentChar + "'\n" + stringValue + "\n" + debugGetRemainingString)
    }
    nextChar()
  }

  def consumeArrayValuesSeparator(): Unit = {
    skipWhitespaceChars()
    if (currentChar != ',') {
      throw new IllegalArgumentException("Array values should be separated by ',' symbol but was '" + currentChar + "'\n" + stringValue + "\n" + debugGetRemainingString)
    }
    nextChar()
  }

  def debugGetRemainingString: String = {
    stringValue.substring(nextIndex - 1)
  }

  def debugGetConsumedString: String = {
    stringValue.substring(0, nextIndex - 1)
  }

}
