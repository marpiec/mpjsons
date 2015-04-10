package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.{JsonTypeDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.StringIterator

/**
 * @author Marcin Pieciukiewicz
 */
object BooleanDeserializer extends JsonTypeDeserializer[Boolean] {
  def deserialize(jsonIterator: StringIterator, clazz: Class[Boolean], field: Field): Boolean = {

    val booleanString = new StringBuilder()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.isCurrentCharASmallLetter) {
      booleanString.append(jsonIterator.currentChar)
      jsonIterator.nextChar()
    }

    booleanString.toString() match {
      case "true" => true
      case "false" => false
      case _ => throw new IllegalArgumentException("Invalid boolean value: \""+booleanString+"\"")
    }
  }

}
