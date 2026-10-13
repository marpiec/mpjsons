package io.mpjsons.impl.deserializer.utiltypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple4Deserializer[T1, T2, T3, T4](val deserializerFactory: DeserializerFactory, val tpe: Type, context: Context)
  extends JsonTypeDeserializer[(T1, T2, T3, T4)] {

  private val types = TypesUtil.getMultipleSubElementsType(tpe)

  private val firstDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T1](types.head, context), types.head)
  private val secondDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T2](types(1), context), types(1))
  private val thirdDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T3](types(2), context), types(2))
  private val forthDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T4](types(3), context), types(3))


  def deserialize(jsonIterator: StringIterator): (T1, T2, T3, T4) = {

    jsonIterator.consumeArrayStart()
    val first = firstDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val second = secondDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val third = thirdDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val forth = forthDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayEnd()

    (first, second, third, forth)
  }
}
