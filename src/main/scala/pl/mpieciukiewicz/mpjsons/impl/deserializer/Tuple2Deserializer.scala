package pl.mpieciukiewicz.mpjsons.impl.deserializer

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class Tuple2Deserializer[T1, T2](val deserializerFactory: DeserializerFactory, val tpe: Type)
  extends JsonTypeDeserializer[(T1, T2)] {

  val (firstType, secondType): (Type, Type) = TypesUtil.getDoubleSubElementsType(tpe)
  val firstDeserializer = deserializerFactory.getDeserializer[T1](firstType)
  val secondDeserializer = deserializerFactory.getDeserializer[T2](secondType)


  def deserialize(jsonIterator: StringIterator): (T1, T2) = {

    jsonIterator.consumeArrayStart()
    val first = firstDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayValuesSeparator()
    val second = secondDeserializer.deserialize(jsonIterator)
    jsonIterator.consumeArrayEnd()

    (first, second)
  }
}
