package pl.marpiec.mpjsons.impl.deserializer.primitives

import java.lang.reflect.Field
import pl.marpiec.mpjsons.{StringIterator, JsonTypeDeserializer}

/**
 * @author Marcin Pieciukiewicz
 */
object BooleanDeserializer extends JsonTypeDeserializer[Boolean] {
  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): Boolean = {

    val booleanString = new StringBuilder()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.isCurrentCharASmallLetter) {
      booleanString.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    booleanString.toBoolean
  }

}
