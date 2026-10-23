package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.ListSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ListSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, ListSet[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): ListSet[E] = {
    deserializeInto(jsonIterator, ListSet.newBuilder[E])
  }

}