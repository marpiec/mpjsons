package pl.mpieciukiewicz.mpjsons.impl.deserializer

import java.lang.reflect.Field

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.util.{TypesUtil, ObjectConstructionUtil}
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil
import pl.mpieciukiewicz.mpjsons.impl.{JsonInnerException, DeserializerFactory, StringIterator}

/**
 * @author Marcin Pieciukiewicz
 */

object EitherDeserializer extends JsonTypeDeserializer[Any] {

  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): Any = {

    jsonIterator.consumeObjectStart()

    val types = TypesUtil.getDoubleSubElementsType(field)

    val identifier = IdentifierDeserializer.deserialize(jsonIterator)

    val elementType = if(identifier == "left") {
      types._1
    } else if(identifier == "right") {
      types._2
    } else {
      throw new JsonInnerException("Incorrect identifier in Either " + identifier, null)
    }

    jsonIterator.consumeFieldValueSeparator()

    val deserializer = DeserializerFactory.getDeserializer(elementType)
    val value = deserializer.deserialize(jsonIterator, elementType, field)

    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == ',') {
      jsonIterator.nextChar()
    }

    jsonIterator.nextCharOrNullIfLast

    if(identifier == "left") {
      Left(value)
    } else {
      Right(value)
    }
  }
}
