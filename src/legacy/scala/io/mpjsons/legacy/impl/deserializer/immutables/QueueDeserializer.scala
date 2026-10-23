package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.Queue
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class QueueDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, Queue[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): Queue[E] = {
    deserializeInto(jsonIterator, Queue.newBuilder[E])
  }

}
