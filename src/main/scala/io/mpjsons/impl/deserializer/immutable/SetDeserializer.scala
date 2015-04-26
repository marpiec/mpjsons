package io.mpjsons.impl.deserializer.immutable

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[E, Set[E]](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): Set[E] = {
    Set(deserializeArray(jsonIterator, tpe): _*)
  }

}