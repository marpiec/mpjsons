package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator

/**
 * @author Marcin Pieciukiewicz
 */
abstract class AbstractIntegerDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readNumberString(jsonIterator: StringIterator): String = {

    jsonIterator.skipWhitespaceChars()

    val identifier = new StringBuilder()

    while (jsonIterator.isCurrentCharADigitPart) {
      identifier.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    identifier.toString()
  }


}