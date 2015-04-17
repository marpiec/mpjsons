package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.StringDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
object CharDeserializer extends JsonTypeDeserializer[Char] {

  def deserialize(jsonIterator: StringIterator, tpe: Type)(implicit deserializerFactory: DeserializerFactory): Char = {

    val deserializedString: String = StringDeserializer.deserialize(jsonIterator, tpe)

    if (deserializedString.length == 1) {
      deserializedString.charAt(0)
    } else {
      throw new IllegalArgumentException("Char value have to be 1 character long: [" + deserializedString + " ]")
    }
  }

}
