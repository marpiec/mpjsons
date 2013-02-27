package pl.marpiec.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import pl.marpiec.mpjsons.{StringIterator, JsonTypeDeserializer}
import pl.marpiec.mpjsons.impl.{DeserializerFactory}

/**
 * @author Marcin Pieciukiewicz
 */

object MapDeserializer extends JsonTypeDeserializer[Map[_, _]] {


  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): Map[_, _] = {

    jsonIterator.consumeArrayStart

    val (keyType, valueType) = TypesUtil.getDoubleSubElementsType(field)
    var map = Map[Any, Any]()

    jsonIterator.skipWhitespaceChars

    while (jsonIterator.currentChar != ']') {

      jsonIterator.consumeObjectStart

      val firstIdentifier = IdentifierDeserializer.deserialize(jsonIterator)

      jsonIterator.consumeFieldValueSeparator //skip ":"

      val firstValue = deserializeValue(jsonIterator, field, firstIdentifier, keyType, valueType)

      jsonIterator.consumeArrayValuesSeparator


      val secondIdentifier = IdentifierDeserializer.deserialize(jsonIterator)

      if (secondIdentifier == firstIdentifier) {
        throw new IllegalArgumentException("Map entry elements must be 'k' and 'v' but both was [" + firstIdentifier + "]")
      }

      jsonIterator.consumeFieldValueSeparator //skip ":"

      val secondValue = deserializeValue(jsonIterator, field, secondIdentifier, keyType, valueType)


      if (firstIdentifier == "k") {
        map += firstValue -> secondValue
      } else {
        map += secondValue -> firstValue
      }

      jsonIterator.consumeObjectEnd // }
      jsonIterator.skipWhitespaceChars

      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar
      }

    }

    jsonIterator.nextChar
    map
  }

  def deserializeValue(jsonIterator: StringIterator, field: Field, identifier: String,
                       keyType: Class[_], valueType: Class[_]): Any = {
    if (identifier == "k") {
      DeserializerFactory.getDeserializer(keyType).deserialize(jsonIterator, keyType, field)
    } else if (identifier == "v") {
      DeserializerFactory.getDeserializer(valueType).deserialize(jsonIterator, valueType, field)
    } else {
      throw new IllegalArgumentException("Map entry should contain k or v element but was: [" + identifier + "]")
    }
  }


}
