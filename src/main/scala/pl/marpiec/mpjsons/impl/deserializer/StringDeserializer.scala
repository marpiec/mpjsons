package pl.marpiec.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.marpiec.mpjsons.{JsonTypeDeserializer}
import pl.marpiec.mpjsons.impl.StringIterator

/**
 * @author Marcin Pieciukiewicz
 */

object StringDeserializer extends JsonTypeDeserializer[String] {
  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): String = {

    jsonIterator.skipWhitespaceChars()

    if (jsonIterator.currentChar != '"') {
      throw new IllegalArgumentException("String value shuld start with '\"', but was [" + jsonIterator.currentChar + "]")
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
          case _ => throw new IllegalArgumentException("Unsupported control character [\\" + jsonIterator.currentChar + "]")
        }

      } else {
        stringValue.append(jsonIterator.currentChar)
      }

      jsonIterator.nextChar()
    }

    jsonIterator.nextChar() //to pass closing ", it is " for sure because of previous while

    stringValue.toString()
  }

}
