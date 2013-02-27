package pl.marpiec.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.{ReflectionUtil, ObjectConstructionUtil}
import pl.marpiec.mpjsons.{StringIterator, JsonTypeDeserializer}
import pl.marpiec.mpjsons.impl.{DeserializerFactory}

/**
 * @author Marcin Pieciukiewicz
 */

object BeanDeserializer extends JsonTypeDeserializer[Any] {

  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): Any = {

    jsonIterator.consumeObjectStart

    val instance = ObjectConstructionUtil.createInstance(clazz)

    while (jsonIterator.currentChar != '}') {


      val identifier = IdentifierDeserializer.deserialize(jsonIterator)

      val field = ReflectionUtil.getField(clazz, identifier)
      field.setAccessible(true)

      val fieldType = field.getType

      jsonIterator.consumeFieldValueSeparator

      val deserializer = DeserializerFactory.getDeserializer(fieldType)

      val value = deserializer.deserialize(jsonIterator, fieldType, field)

      field.set(instance, value)

      jsonIterator.skipWhitespaceChars
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar
      }
    }

    jsonIterator.nextCharOrNullIfLast

    instance
  }
}
