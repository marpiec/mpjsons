package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.{StringIterator, DeserializerFactory}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

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