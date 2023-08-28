package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.ListSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ListSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, ListSet[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): ListSet[E] = {
    ListSet.from(deserializeArray(jsonIterator, tpe))
  }

}