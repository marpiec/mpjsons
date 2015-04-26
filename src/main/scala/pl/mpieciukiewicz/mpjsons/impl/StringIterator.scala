package io.mpjsons.impl

/**
 * Special container for String that allows easy iteration through String characters.
 * @author Marcin Pieciukiewicz
 */
class StringIterator(private val stringValue: String) {

  val stringLength = stringValue.length
  var nextIndex = 1
  var currentChar: Char = stringValue.charAt(0)

  def skipWhitespaceChars(): Unit = {
    while (currentChar.isWhitespace) {
      nextChar()
    }
  }

  def nextChar(): Unit = {
    currentChar = if (nextIndex < stringLength) {
      stringValue.charAt(nextIndex)
    } else {
      '\u0000'
    }
    nextIndex = nextIndex + 1
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
    currentChar >= '0' && currentChar <= '9' || currentChar == '-' || currentChar == '.'
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
