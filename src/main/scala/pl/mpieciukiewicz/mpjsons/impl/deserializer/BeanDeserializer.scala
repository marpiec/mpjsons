package pl.mpieciukiewicz.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.{ObjectConstructionUtil}
import pl.mpieciukiewicz.mpjsons.{JsonTypeDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil

/**
 * @author Marcin Pieciukiewicz
 */

object BeanDeserializer extends JsonTypeDeserializer[AnyRef] {

  def deserialize(jsonIterator: StringIterator, clazz: Class[AnyRef], field: Field): AnyRef = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.createInstance(clazz)

    while (jsonIterator.currentChar != '}') {

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      val field = ReflectionUtil.getAccessibleField(clazz, identifier)
      val fieldType = field.getType.asInstanceOf[Class[Any]]

      jsonIterator.consumeFieldValueSeparator()

      val deserializer = DeserializerFactory.getDeserializer(fieldType)
      val value = deserializer.deserialize(jsonIterator, fieldType, field)

      field.set(instance, value)

      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }
    }

    jsonIterator.nextCharOrNullIfLast

    instance
  }
}
