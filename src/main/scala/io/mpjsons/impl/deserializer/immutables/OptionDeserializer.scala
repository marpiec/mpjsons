package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * Reads `{}` as None, `{"value":v}` as Some(v) and the `null` literal as None.
 * @author Marcin Pieciukiewicz
 */

class OptionDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context) extends JsonTypeDeserializer[Option[E]] {

  private val subType = TypesUtil.getSubElementsType(tpe)
  private val subTypeDeserializer = deserializerFactory.getDeserializer(subType, context).asInstanceOf[JsonTypeDeserializer[E]]
  private val subTypeNull = NullValues.forType(subType)

  override def deserialize(jsonIterator: StringIterator): Option[E] = {

    if (jsonIterator.tryConsumeNull()) {
      return None
    }

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
      val value = NullValues.read(jsonIterator, subTypeNull, subTypeDeserializer)
      Some[E](value)
    }

    jsonIterator.skipWhitespaceChars()
    jsonIterator.nextCharOrNullIfLast
    option
  }
}
