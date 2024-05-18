package io.mpjsons.impl.deserializer.utiltypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple5Deserializer[T1, T2, T3, T4, T5](val deserializerFactory: DeserializerFactory, val tpe: Type, context: Context)
  extends JsonTypeDeserializer[(T1, T2, T3, T4, T5)] {

  private val types = TypesUtil.getMultipleSubElementsType(tpe)

  private val firstDeserializer = deserializerFactory.getDeserializer[T1](types.head, context)
  private val secondDeserializer = deserializerFactory.getDeserializer[T2](types(1), context)
  private val thirdDeserializer = deserializerFactory.getDeserializer[T3](types(2), context)
  private val forthDeserializer = deserializerFactory.getDeserializer[T4](types(3), context)
  private val fifthDeserializer = deserializerFactory.getDeserializer[T5](types(4), context)


  def deserialize(jsonIterator: StringIterator): (T1, T2, T3, T4, T5) = {

    jsonIterator.consumeArrayStart()
    val first = firstDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val second = secondDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val third = thirdDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val forth = forthDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val fifth = fifthDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayEnd()

    (first, second, third, forth, fifth)
  }
}