package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * Deserializer for LazyList (the 2.13 replacement of Stream). The JSON array is read eagerly; the result is a
 * fully evaluated LazyList.
 */
class LazyListDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, LazyList[E]](deserializerFactory, tpe, context) {

  override def deserialize(jsonIterator: StringIterator): LazyList[E] = {
    deserializeInto(jsonIterator, LazyList.newBuilder[E])
  }

}
