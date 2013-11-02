package pl.marpiec.mpjsons.impl.serializer

import java.lang.reflect.AccessibleObject
import pl.marpiec.mpjsons.JsonTypeSerializer
import pl.marpiec.mpjsons.impl.SerializerFactory
import pl.marpiec.mpjsons.impl.util.reflection.ReflectionUtil

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
        SerializerFactory.getSerializer(value).serialize(value, jsonBuilder)
      }
    }


    jsonBuilder.append('}')
  }


}
