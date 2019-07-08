package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator


object AbstractFloatingPointDeserializer {
  def readNumberString(jsonIterator: StringIterator): StringBuilder = {

    jsonIterator.skipWhitespaceChars()

    val identifier = new StringBuilder()

    while (jsonIterator.isCurrentCharAFloatingPointPart) {
      identifier.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }
    identifier
  }
}

/**
 * @author Marcin Pieciukiewicz
 */
abstract class AbstractFloatingPointDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readNumberString(jsonIterator: StringIterator): String = {
    AbstractFloatingPointDeserializer.readNumberString(jsonIterator).toString()
  }

}