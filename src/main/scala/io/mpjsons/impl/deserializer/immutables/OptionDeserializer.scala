package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */

class OptionDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, Option[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): Option[E] = {
    deserializeArray(jsonIterator, tpe).headOption
  }
}
