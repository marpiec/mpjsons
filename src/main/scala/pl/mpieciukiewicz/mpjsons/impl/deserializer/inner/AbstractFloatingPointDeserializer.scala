package pl.mpieciukiewicz.mpjsons.impl.deserializer.inner

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
abstract class AbstractFloatingPointDeserializer[T] extends JsonTypeDeserializer[T] {

  protected def readNumberString(jsonIterator: StringIterator): String = {

    jsonIterator.skipWhitespaceChars()

    val identifier = new StringBuilder()

    while (jsonIterator.isCurrentCharAFloatingPointPart) {
      identifier.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }
    identifier.toString()
  }

}