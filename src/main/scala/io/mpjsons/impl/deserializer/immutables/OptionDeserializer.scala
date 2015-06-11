package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class OptionDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context) extends JsonTypeDeserializer[Option[E]] {

  val subTypeDeserializer = deserializerFactory.getDeserializer(TypesUtil.getSubElementsType(tpe), context).asInstanceOf[JsonTypeDeserializer[E]]

  override def deserialize(jsonIterator: StringIterator): Option[E] = {

    jsonIterator.consumeObjectStart()

    jsonIterator.skipWhitespaceChars()

    val option = if (jsonIterator.currentChar == '}') {
      None
    } else {
      val identifier = IdentifierDeserializer.deserialize(jsonIterator)

      if (identifier != "value") {
        throw new IllegalArgumentException("Expected 'value' but was = " + identifier)
      }

      jsonIterator.consumeFieldValueSeparator()
      val value = subTypeDeserializer.deserialize(jsonIterator)
      Some[E](value)
    }

    jsonIterator.skipWhitespaceChars()
    jsonIterator.nextCharOrNullIfLast
    option
  }
}
