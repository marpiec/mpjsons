package pl.mpieciukiewicz.mpjsons.impl.deserializer.immutable

import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ListDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[E, List[E]](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): List[E] = {
    deserializeArray(jsonIterator, tpe).toList
  }

}
