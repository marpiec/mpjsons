package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.{StringIterator, DeserializerFactory}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.immutable.HashSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class HashSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[E, HashSet[E]](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): HashSet[E] = {
    HashSet(deserializeArray(jsonIterator, tpe): _*)
  }

}