package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import pl.mpieciukiewicz.mpjsons.impl.{StringIterator, DeserializerFactory}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

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
