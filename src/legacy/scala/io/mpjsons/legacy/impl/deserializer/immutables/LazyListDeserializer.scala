package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

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
