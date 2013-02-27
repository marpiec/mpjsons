package pl.marpiec.mpjsons.impl.deserializer.inner

import java.lang.reflect.Field
import pl.marpiec.mpjsons.{StringIterator, JsonTypeDeserializer}

/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractFloatingPointDeserializer[T] extends JsonTypeDeserializer[T] {

  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): T = {

    jsonIterator.skipWhitespaceChars

    val identifier = new StringBuilder()

    while (jsonIterator.isCurrentCharAFloatingPointPart) {
      identifier.append(jsonIterator.currentChar)
      jsonIterator.nextChar
    }

    toProperFloatingPoint(identifier)
  }

  protected def toProperFloatingPoint(identifier: StringBuilder): T


}