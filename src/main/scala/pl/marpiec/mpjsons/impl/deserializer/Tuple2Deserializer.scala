package pl.marpiec.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import pl.marpiec.mpjsons.{JsonTypeDeserializer}
import pl.marpiec.mpjsons.impl.{DeserializerFactory, StringIterator}


/**
 * @author Marcin Pieciukiewicz
 */

object Tuple2Deserializer extends JsonTypeDeserializer[(_, _)] {
  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): (_, _) = {

    jsonIterator.consumeArrayStart()

    val (firstElementType, secondElementType) = TypesUtil.getDoubleSubElementsType(field)

    val first = DeserializerFactory.getDeserializer(firstElementType).deserialize(jsonIterator, firstElementType, field)

    jsonIterator.consumeArrayValuesSeparator()

    val second = DeserializerFactory.getDeserializer(secondElementType).deserialize(jsonIterator, secondElementType, field)

    jsonIterator.consumeArrayEnd()

    (first, second)
  }
}
