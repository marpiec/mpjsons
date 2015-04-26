package io.mpjsons.impl.deserializer.immutable

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.Queue
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class QueueDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[E, Queue[E]](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): Queue[E] = {
    Queue(deserializeArray(jsonIterator, tpe):_*)
  }

}
