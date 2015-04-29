package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class VectorDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Map[Symbol, Type])
  extends AbstractJsonArrayDeserializer[E, Vector[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): Vector[E] = {
    deserializeArray(jsonIterator, tpe).toVector
  }

}