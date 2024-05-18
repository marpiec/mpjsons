package io.mpjsons.impl.deserializer.utiltypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple1Deserializer[T1](val deserializerFactory: DeserializerFactory, val tpe: Type, context: Context)
  extends JsonTypeDeserializer[Tuple1[T1]] {

  private val firstType: Type = TypesUtil.getSubElementsType(tpe)
  private val firstDeserializer = deserializerFactory.getDeserializer[T1](firstType, context)


  def deserialize(jsonIterator: StringIterator): Tuple1[T1] = {

    jsonIterator.consumeArrayStart()
    val first = firstDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayEnd()

    Tuple1(first)
  }
}
