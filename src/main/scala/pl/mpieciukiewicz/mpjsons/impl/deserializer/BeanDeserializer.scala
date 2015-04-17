package pl.mpieciukiewicz.mpjsons.impl.deserializer

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil
import pl.mpieciukiewicz.mpjsons.impl.util.{ObjectConstructionUtil, TypesUtil}
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object BeanDeserializer extends JsonTypeDeserializer[AnyRef] {

  def deserialize(jsonIterator: StringIterator, tpe: Type)
                 (implicit deserializerFactory: DeserializerFactory): AnyRef = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.createInstance(TypesUtil.getClassFromType(tpe))

    while (jsonIterator.currentChar != '}') {

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      val fieldInfo = ReflectionUtil.getAccessibleField(tpe, identifier)

      jsonIterator.consumeFieldValueSeparator()

      val deserializer = deserializerFactory.getDeserializer(fieldInfo.tpe).asInstanceOf[JsonTypeDeserializer[Any]]
      val value = deserializer.deserialize(jsonIterator, fieldInfo.tpe)

      fieldInfo.field.set(instance, value)

      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }
    }

    jsonIterator.nextCharOrNullIfLast

    instance
  }

}
