package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator

/**
 * @author Marcin Pieciukiewicz
 */

object AbstractStringDeserializer {
  def readString(jsonIterator: StringIterator): StringBuilder = {

    jsonIterator.skipWhitespaceChars()

    if (jsonIterator.currentChar != '"') {
      throw new IllegalArgumentException("String value should start with '\"', but was [" + jsonIterator.currentChar + "]")
    }

    val stringValue = new StringBuilder()

    jsonIterator.nextChar()

    while (jsonIterator.currentChar != '"') {

      if (jsonIterator.currentChar == '\\') {
        jsonIterator.nextChar()

        val char = jsonIterator.currentChar

        char match {
          case '"' => stringValue.append('"')
          case '\\' => stringValue.append('\\')
          case '/' => stringValue.append('/')
          case 'b' => stringValue.append('\b')
          case 'f' => stringValue.append('\f')
          case 'n' => stringValue.append('\n')
          case 'r' => stringValue.append('\r')
          case 't' => stringValue.append('\t')
          case 'u' =>
            var i = 0
            var numberText = new StringBuilder()
            while(i < 4) {
              jsonIterator.nextChar()
              numberText += jsonIterator.currentChar
              i += 1
            }
            val char = String.valueOf(Character.toChars(Integer.parseInt(numberText.toString(), 16)))
            stringValue.append(char)
          case _ => throw new IllegalArgumentException("Unsupported control character [\\" + jsonIterator.currentChar + "]")
        }

      } else {
        stringValue.append(jsonIterator.currentChar)
      }

      jsonIterator.nextChar()
    }

    jsonIterator.nextChar() //to pass closing ", it is " for sure because of previous while

    stringValue
  }
}

abstract class AbstractStringDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readString(jsonIterator: StringIterator): String = {
    AbstractStringDeserializer.readString(jsonIterator).toString()
  }
}
