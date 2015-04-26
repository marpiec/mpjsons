package pl.mpieciukiewicz.mpjsons.impl.deserializer.immutable

import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.ListSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ListSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[E, ListSet[E]](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): ListSet[E] = {
    ListSet(deserializeArray(jsonIterator, tpe): _*)
  }

}