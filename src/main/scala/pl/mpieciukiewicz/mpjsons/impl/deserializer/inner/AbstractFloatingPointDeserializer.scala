package pl.mpieciukiewicz.mpjsons.impl.deserializer.inner

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractFloatingPointDeserializer[T] extends JsonTypeDeserializer[T] {

  override def deserialize(jsonIterator: StringIterator, tpe: Type)
                          (implicit deserializerFactory: DeserializerFactory): T = {

    jsonIterator.skipWhitespaceChars()

    val identifier = new StringBuilder()

    while (jsonIterator.isCurrentCharAFloatingPointPart) {
      identifier.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    toProperFloatingPoint(identifier)
  }

  protected def toProperFloatingPoint(identifier: StringBuilder): T


}