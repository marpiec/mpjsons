package pl.mpieciukiewicz.mpjsons.impl.deserializer.immutable

import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

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