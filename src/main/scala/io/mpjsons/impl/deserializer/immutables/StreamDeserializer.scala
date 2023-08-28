package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.IterableFactory.toBuildFrom
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class StreamDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, Stream[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): Stream[E] = {
    Stream.fromIterator(deserializeArray(jsonIterator, tpe).iterator)
  }

}