package pl.marpiec.mpjsons.impl.deserializer.primitives

import java.lang.reflect.Field
import pl.marpiec.mpjsons.{JsonTypeDeserializer}
import pl.marpiec.mpjsons.impl.StringIterator

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

    booleanString.toString() match {
      case "true" => true
      case "false" => false
      case _ => throw new IllegalArgumentException("Invalid boolean value: \""+booleanString+"\"")
    }
  }

}
