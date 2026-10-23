package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.HashSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class HashSetDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, HashSet[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): HashSet[E] = {
    deserializeInto(jsonIterator, HashSet.newBuilder[E])
  }

}