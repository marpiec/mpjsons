package pl.mpieciukiewicz.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.{JsonTypeDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}


/**
 * @author Marcin Pieciukiewicz
 */

object Tuple2Deserializer extends JsonTypeDeserializer[(Any, Any)] {
  def deserialize(jsonIterator: StringIterator, clazz: Class[(Any, Any)], field: Field): (Any, Any) = {

    jsonIterator.consumeArrayStart()

    val (firstElementType, secondElementType): (Class[Any], Class[Any]) = TypesUtil.getDoubleSubElementsType(field)

    val first = DeserializerFactory.getDeserializer(firstElementType).deserialize(jsonIterator, firstElementType, field)

    jsonIterator.consumeArrayValuesSeparator()

    val second = DeserializerFactory.getDeserializer(secondElementType).deserialize(jsonIterator, secondElementType, field)

    jsonIterator.consumeArrayEnd()

    (first, second)
  }
}
