package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class IterableDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, Iterable[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): Iterable[E] = {
    deserializeArray(jsonIterator, tpe)
  }

}
