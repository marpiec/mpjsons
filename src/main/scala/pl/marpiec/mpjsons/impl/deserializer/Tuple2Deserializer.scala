package pl.marpiec.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import pl.marpiec.mpjsons.{StringIterator, JsonTypeDeserializer}
import pl.marpiec.mpjsons.impl.{DeserializerFactory}


/**
 * @author Marcin Pieciukiewicz
 */

object Tuple2Deserializer extends JsonTypeDeserializer[Tuple2[_, _]] {
  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): Tuple2[_, _] = {

    jsonIterator.consumeArrayStart

    val (firstElementType, secondElementType) = TypesUtil.getDoubleSubElementsType(field)

    val first = DeserializerFactory.getDeserializer(firstElementType).deserialize(jsonIterator, firstElementType, field)

    jsonIterator.consumeArrayValuesSeparator

    val second = DeserializerFactory.getDeserializer(secondElementType).deserialize(jsonIterator, secondElementType, field)

    jsonIterator.consumeArrayEnd

    (first, second)
  }
}
