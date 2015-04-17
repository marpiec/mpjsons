package pl.mpieciukiewicz.mpjsons.impl.deserializer

import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object Tuple2Deserializer extends JsonTypeDeserializer[(Any, Any)] {
  def deserialize(jsonIterator: StringIterator, tpe: Type)(implicit deserializerFactory: DeserializerFactory): (Any, Any) = {

    jsonIterator.consumeArrayStart()

    val (firstElementType, secondElementType): (Type, Type) = TypesUtil.getDoubleSubElementsType(tpe)

    val first = deserializerFactory.getDeserializer[Any](firstElementType).deserialize(jsonIterator, firstElementType)

    jsonIterator.consumeArrayValuesSeparator()

    val second = deserializerFactory.getDeserializer[Any](secondElementType).deserialize(jsonIterator, secondElementType)

    jsonIterator.consumeArrayEnd()

    (first, second)
  }
}
