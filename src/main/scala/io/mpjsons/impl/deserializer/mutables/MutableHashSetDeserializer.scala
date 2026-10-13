package io.mpjsons.impl.deserializer.mutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable.HashSet
import scala.reflect.runtime.universe._

/**
 * Deserializer for mutable.HashSet and mutable.Set.
 */
class MutableHashSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, HashSet[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): HashSet[E] = {
    HashSet.from(deserializeArray(jsonIterator, tpe))
  }

}
