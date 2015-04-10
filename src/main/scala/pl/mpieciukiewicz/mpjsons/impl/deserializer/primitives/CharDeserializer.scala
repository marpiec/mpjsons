package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.{JsonTypeDeserializer}
import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.deserializer.StringDeserializer
import pl.mpieciukiewicz.mpjsons.impl.StringIterator

/**
 * @author Marcin Pieciukiewicz
 */
object CharDeserializer extends JsonTypeDeserializer[Char] {

  def deserialize(jsonIterator: StringIterator, clazz: Class[Char], field: Field): Char = {

    val deserializedString: String = StringDeserializer.deserialize(jsonIterator, classOf[String], field)

    if(deserializedString.length == 1) {
      deserializedString.charAt(0)
    } else {
      throw new IllegalArgumentException("Char value have to be 1 character long: [" + deserializedString + " ]")
    }
  }

}
