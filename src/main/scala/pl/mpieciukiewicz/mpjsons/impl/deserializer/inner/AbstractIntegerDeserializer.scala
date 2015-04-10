package pl.mpieciukiewicz.mpjsons.impl.deserializer.inner

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.ClassType
import pl.mpieciukiewicz.mpjsons.{JsonTypeDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractIntegerDeserializer[T] extends JsonTypeDeserializer[T] {

  override def deserialize(jsonIterator: StringIterator, classType: ClassType): T = {

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