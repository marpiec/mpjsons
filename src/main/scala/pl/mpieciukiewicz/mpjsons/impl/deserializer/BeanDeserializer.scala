package pl.mpieciukiewicz.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.{ClassType, ObjectConstructionUtil}
import pl.mpieciukiewicz.mpjsons.{JsonTypeDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil
import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

object BeanDeserializer extends JsonTypeDeserializer[AnyRef] {



  def deserialize(jsonIterator: StringIterator, classType: ClassType): AnyRef = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.createInstance(classType.clazz)

    while (jsonIterator.currentChar != '}') {

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      val fieldInfo = ReflectionUtil.getAccessibleField(classType, identifier)
      val fieldType = fieldInfo.field.getType.asInstanceOf[Class[Any]]

      jsonIterator.consumeFieldValueSeparator()

      val deserializer = DeserializerFactory.getDeserializer(fieldType)
      val value = deserializer.deserialize(jsonIterator, ClassType(fieldType, fieldInfo.tpe))

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
