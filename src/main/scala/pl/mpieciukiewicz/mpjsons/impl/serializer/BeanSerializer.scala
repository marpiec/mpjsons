package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory

/**
 * @author Marcin Pieciukiewicz
 */

object BeanSerializer extends JsonTypeSerializer[AnyRef] {


  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder)
                        (implicit serializerFactory: SerializerFactory) = {

    jsonBuilder.append('{')

    val clazz = obj.getClass
    val fields = ReflectionUtil.getAllAccessibleFields(TypesUtil.getTypeFromClass(obj.getClass))


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
        serializerFactory.getSerializer(TypesUtil.getTypeFromClass(value.getClass))
          .asInstanceOf[JsonTypeSerializer[AnyRef]].serialize(value, jsonBuilder)
      }
    }


    jsonBuilder.append('}')
  }


}
