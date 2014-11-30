package pl.mpieciukiewicz.mpjsons.impl.serializer

import java.lang.reflect.AccessibleObject
import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object BeanSerializer extends JsonTypeSerializer {


  def serialize(obj: Any, jsonBuilder: StringBuilder) = {

    jsonBuilder.append('{')

    val clazz = obj.asInstanceOf[AnyRef].getClass
    val fields = ReflectionUtil.getAllAccessibleFields(clazz)


    var isNotFirstField = false

    for (field <- fields) {

      val value = field.get(obj)
      if (value != null) {

        if (isNotFirstField) {
          jsonBuilder.append(",")
        } else {
          isNotFirstField = true
        }

        jsonBuilder.append('"').append(field.getName).append('"').append(':')
        SerializerFactory.getSerializer(value.getClass).serialize(value, jsonBuilder)
      }
    }


    jsonBuilder.append('}')
  }


}
