package io.mpjsons.impl.deserializer

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.values.StringDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object IdentifierDeserializer {

  def deserialize(jsonIterator: StringIterator): String = {

    jsonIterator.skipWhitespaceChars()

    val quoted = jsonIterator.currentChar.equals('"')

    if(quoted) {
      StringDeserializer.deserialize(jsonIterator)
    } else {
      val identifier = new StringBuilder()

      identifier.append(jsonIterator.currentChar)

      jsonIterator.nextChar()

      while (jsonIterator.currentChar != ':' && (!quoted && !jsonIterator.currentChar.isWhitespace || quoted && !jsonIterator.currentChar.equals('"'))) {
        identifier.append(jsonIterator.currentChar)
        jsonIterator.nextChar()
      }

      identifier.toString()

    }

  }

}
