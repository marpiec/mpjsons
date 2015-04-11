package pl.mpieciukiewicz.mpjsons.impl.deserializer

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.{TypesUtil, ClassType, ObjectConstructionUtil}
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
    val instance = ObjectConstructionUtil.createInstance(TypesUtil.getClassFromType(classType.tpe))

    while (jsonIterator.currentChar != '}') {

      val identifier = IdentifierDeserializer.deserialize(jsonIterator)
      val fieldInfo = ReflectionUtil.getAccessibleField(classType, identifier)

      jsonIterator.consumeFieldValueSeparator()

      val deserializer = DeserializerFactory.getDeserializer(fieldInfo.tpe).asInstanceOf[JsonTypeDeserializer[Any]]
      val value = deserializer.deserialize(jsonIterator, ClassType(fieldInfo.tpe))

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
