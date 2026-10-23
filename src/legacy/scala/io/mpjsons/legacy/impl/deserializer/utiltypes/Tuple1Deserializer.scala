package io.mpjsons.legacy.impl.deserializer.utiltypes

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.util.{Context, NullValues, TypesUtil}
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple1Deserializer[T1](val deserializerFactory: DeserializerFactory, val tpe: Type, context: Context)
  extends JsonTypeDeserializer[Tuple1[T1]] {

  private val firstType: Type = TypesUtil.getSubElementsType(tpe)
  private val firstDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T1](firstType, context), firstType, deserializerFactory.config)


  def deserialize(jsonIterator: StringIterator): Tuple1[T1] = {

    jsonIterator.consumeArrayStart()
    val first = firstDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayEnd()

    Tuple1(first)
  }
}
