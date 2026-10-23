package io.mpjsons.legacy.impl.deserializer.mutables

import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.legacy.impl.util.{Context, TypesUtil}
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable.ArrayBuilder
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ArrayDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, Array[E]](deserializerFactory, tpe, context) {

  /** ClassTag for the element class, so primitive element types get primitive arrays without reflection per element. */
  private val elementClassTag: ClassTag[E] = ClassTag[E](TypesUtil.getClassFromType[E](elementsType))

  override def deserialize(jsonIterator: StringIterator): Array[E] = {
    deserializeInto(jsonIterator, ArrayBuilder.make[E](elementClassTag))
  }

}
