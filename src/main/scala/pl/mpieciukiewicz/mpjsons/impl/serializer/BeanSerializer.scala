package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BeanSerializer(serializerFactory: SerializerFactory, tpe: Type) extends JsonTypeSerializer[AnyRef] {

  val fields = ReflectionUtil.getAllAccessibleFields(tpe)
  val fieldsWithSerializers = fields.map {field =>
    (field.field, field.field.getName, serializerFactory.getSerializer(field.tpe)
    .asInstanceOf[JsonTypeSerializer[AnyRef]])}


  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder) = {

    jsonBuilder.append('{')

    var isNotFirstField = false

    for (field <- fieldsWithSerializers) {

      val value = field._1.get(obj)
      if (value != null) {

        if (isNotFirstField) {
          jsonBuilder.append(',')
        } else {
          isNotFirstField = true
        }

        jsonBuilder.append('"').append(field._2).append('"').append(':')
        field._3.serialize(value, jsonBuilder)
      }
    }

    jsonBuilder.append('}')
  }

}
