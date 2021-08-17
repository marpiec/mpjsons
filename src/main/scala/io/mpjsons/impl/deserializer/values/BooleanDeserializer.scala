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

object JavaBooleanDeserializer extends JsonTypeDeserializer[java.lang.Boolean] {

  override def deserialize(jsonIterator: StringIterator): java.lang.Boolean = {
    val booleanString = new StringBuilder()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.isCurrentCharASmallLetter) {
      booleanString.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    booleanString.toString() match {
      case "true" => java.lang.Boolean.TRUE
      case "false" => java.lang.Boolean.FALSE
      case _ => throw new IllegalArgumentException(s"Invalid boolean value: $booleanString")
    }
  }

}