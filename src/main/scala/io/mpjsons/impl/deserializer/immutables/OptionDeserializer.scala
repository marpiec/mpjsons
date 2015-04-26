package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

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
