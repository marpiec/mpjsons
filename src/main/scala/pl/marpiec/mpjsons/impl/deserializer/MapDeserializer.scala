package pl.marpiec.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import pl.marpiec.mpjsons.{JsonTypeDeserializer}
import pl.marpiec.mpjsons.impl.{DeserializerFactory, StringIterator}

/**
 * @author Marcin Pieciukiewicz
 */

object MapDeserializer extends JsonTypeDeserializer[Map[_, _]] {


  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): Map[_, _] = {

    jsonIterator.consumeArrayStart()

    val (keyType, valueType) = TypesUtil.getDoubleSubElementsType(field)
    var map = Map[Any, Any]()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.currentChar != ']') {

      jsonIterator.consumeArrayStart()

      val key = deserializeValue(jsonIterator, field, keyType)

      jsonIterator.consumeArrayValuesSeparator()


      val value = deserializeValue(jsonIterator, field, valueType)

      map += key -> value


      jsonIterator.consumeArrayEnd() // ]
      jsonIterator.skipWhitespaceChars()

      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }

    }

    jsonIterator.nextChar()
    map
  }

  def deserializeValue(jsonIterator: StringIterator, field: Field, valueType: Class[_]): Any = {
    DeserializerFactory.getDeserializer(valueType).deserialize(jsonIterator, valueType, field)
  }


}
