package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.{Map, HashSet}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class HashSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Map[Symbol, Type])
  extends AbstractJsonArrayDeserializer[E, HashSet[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): HashSet[E] = {
    HashSet(deserializeArray(jsonIterator, tpe): _*)
  }

}