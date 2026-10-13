package io.mpjsons.impl.deserializer.utiltypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple5Deserializer[T1, T2, T3, T4, T5](val deserializerFactory: DeserializerFactory, val tpe: Type, context: Context)
  extends JsonTypeDeserializer[(T1, T2, T3, T4, T5)] {

  private val types = TypesUtil.getMultipleSubElementsType(tpe)

  private val firstDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T1](types.head, context), types.head, deserializerFactory.config)
  private val secondDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T2](types(1), context), types(1), deserializerFactory.config)
  private val thirdDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T3](types(2), context), types(2), deserializerFactory.config)
  private val forthDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T4](types(3), context), types(3), deserializerFactory.config)
  private val fifthDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[T5](types(4), context), types(4), deserializerFactory.config)


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