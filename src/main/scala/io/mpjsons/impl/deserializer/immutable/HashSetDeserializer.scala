package io.mpjsons.impl.deserializer.immutable

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

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