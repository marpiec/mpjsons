package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.{Map, ListSet}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ListSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Map[Symbol, Type])
  extends AbstractJsonArrayDeserializer[E, ListSet[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): ListSet[E] = {
    ListSet(deserializeArray(jsonIterator, tpe): _*)
  }

}