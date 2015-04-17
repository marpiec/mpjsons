package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
object BooleanDeserializer extends JsonTypeDeserializer[Boolean] {
  def deserialize(jsonIterator: StringIterator, tpe: Type)
                 (implicit deserializerFactory: DeserializerFactory): Boolean = {

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
