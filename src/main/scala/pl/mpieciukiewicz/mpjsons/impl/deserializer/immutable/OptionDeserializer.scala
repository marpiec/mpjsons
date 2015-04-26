package pl.mpieciukiewicz.mpjsons.impl.deserializer.immutable

import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class OptionDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[E, Option[E]](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): Option[E] = {
    deserializeArray(jsonIterator, tpe).headOption
  }
}
