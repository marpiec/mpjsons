package io.mpjsons.impl.deserializer.utiltypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple2Deserializer[T1, T2](val deserializerFactory: DeserializerFactory, val tpe: Type, context: Context)
  extends JsonTypeDeserializer[(T1, T2)] {

  private val (firstType, secondType): (Type, Type) = TypesUtil.getDoubleSubElementsType(tpe)
  private val firstDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T1](firstType, context), firstType, deserializerFactory.config)
  private val secondDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T2](secondType, context), secondType, deserializerFactory.config)


  def deserialize(jsonIterator: StringIterator): (T1, T2) = {

    jsonIterator.consumeArrayStart()
    val first = firstDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val second = secondDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayEnd()

    (first, second)
  }
}
