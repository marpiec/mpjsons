package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.{Map, Queue}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class QueueDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Map[Symbol, Type])
  extends AbstractJsonArrayDeserializer[E, Queue[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): Queue[E] = {
    Queue(deserializeArray(jsonIterator, tpe):_*)
  }

}
