package io.mpjsons.impl.deserializer.utiltypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple3Deserializer[T1, T2, T3](val deserializerFactory: DeserializerFactory, val tpe: Type, context: Context)
  extends JsonTypeDeserializer[(T1, T2, T3)] {

  private val types = TypesUtil.getMultipleSubElementsType(tpe)

  private val firstDeserializer = deserializerFactory.getDeserializer[T1](types.head, context)
  private val secondDeserializer = deserializerFactory.getDeserializer[T2](types(1), context)
  private val thirdDeserializer = deserializerFactory.getDeserializer[T3](types(2), context)


  def deserialize(jsonIterator: StringIterator): (T1, T2, T3) = {

    jsonIterator.consumeArrayStart()
    val first = firstDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val second = secondDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val third = thirdDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayEnd()

    (first, second, third)
  }
}
