package pl.marpiec.mpjsons.impl.deserializer.inner

import java.lang.reflect.Field
import pl.marpiec.mpjsons.{StringIterator, JsonTypeDeserializer}

/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractIntegerDeserializer[T] extends JsonTypeDeserializer[T] {

  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): T = {

    jsonIterator.skipWhitespaceChars()

    val identifier = new StringBuilder()

    while (jsonIterator.isCurrentCharADigitPart) {
      identifier.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    toProperInteger(identifier)
  }

  protected def toProperInteger(identifier: StringBuilder): T


}