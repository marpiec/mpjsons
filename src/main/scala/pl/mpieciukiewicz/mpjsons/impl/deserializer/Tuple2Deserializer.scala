package pl.mpieciukiewicz.mpjsons.impl.deserializer

import pl.mpieciukiewicz.mpjsons.impl.util.{ClassType, TypesUtil}
import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}


/**
 * @author Marcin Pieciukiewicz
 */

object Tuple2Deserializer extends JsonTypeDeserializer[(Any, Any)] {
  def deserialize(jsonIterator: StringIterator, classType: ClassType)(implicit deserializerFactory: DeserializerFactory): (Any, Any) = {

    jsonIterator.consumeArrayStart()

    val (firstElementType, secondElementType): (ClassType, ClassType) = TypesUtil.getDoubleSubElementsType(classType.tpe)

    val first = deserializerFactory.getDeserializer[Any](firstElementType.tpe).deserialize(jsonIterator, firstElementType)

    jsonIterator.consumeArrayValuesSeparator()

    val second = deserializerFactory.getDeserializer[Any](secondElementType.tpe).deserialize(jsonIterator, secondElementType)

    jsonIterator.consumeArrayEnd()

    (first, second)
  }
}
