package io.mpjsons.impl.deserializer.mutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * Deserializer for mutable.ArrayBuffer and the mutable Seq/Buffer/IndexedSeq/Iterable abstractions it implements.
 */
class ArrayBufferDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, ArrayBuffer[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): ArrayBuffer[E] = {
    deserializeArray(jsonIterator, tpe)
  }

}
