package io.mpjsons.impl.deserializer

import io.mpjsons.impl.StringIterator

/**
 * @author Marcin Pieciukiewicz
 */

object IdentifierDeserializer {

  def deserialize(jsonIterator: StringIterator): String = {

    jsonIterator.skipWhitespaceChars()

    val identifier = new StringBuilder()

    val quoted = jsonIterator.currentChar.equals('"')

    if (!quoted) {
      identifier.append(jsonIterator.currentChar)
    }

    jsonIterator.nextChar()

    while (jsonIterator.currentChar != ':' && (!quoted && !jsonIterator.currentChar.isWhitespace || quoted && !jsonIterator.currentChar.equals('"'))) {
      identifier.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    if (quoted) {
      jsonIterator.nextChar() // skip closing quote
    }

    identifier.toString()
  }

}
