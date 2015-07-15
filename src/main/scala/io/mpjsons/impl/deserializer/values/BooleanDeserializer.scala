package io.mpjsons.impl.deserializer.values

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator

/**
 * @author Marcin Pieciukiewicz
 */
object BooleanDeserializer extends JsonTypeDeserializer[Boolean] {

  override def deserialize(jsonIterator: StringIterator): Boolean = {
    val booleanString = new StringBuilder()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.isCurrentCharASmallLetter) {
      booleanString.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    booleanString.toString() match {
      case "true" => true
      case "false" => false
      case _ => throw new IllegalArgumentException(s"Invalid boolean value: $booleanString")
    }
  }

}
