package pl.mpieciukiewicz.mpjsons.impl.deserializer.inner

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractIntegerDeserializer[T] extends JsonTypeDeserializer[T] {

  override def deserialize(jsonIterator: StringIterator, tpe: Type)
                          (implicit deserializerFactory: DeserializerFactory): T = {

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