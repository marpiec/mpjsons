package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.reflection.ReflectionUtil

import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */

class BeanSerializer(serializerFactory: SerializerFactory, private val tpe: Type, private val context: Context) extends JsonTypeSerializer[AnyRef] {

  private val fields = ReflectionUtil.getAllAccessibleFields(tpe)
  /** Lazy val to prevent StackOverflow while construction recursive type serializer */
  private lazy val fieldsWithSerializers = fields.map {field =>
    (field.field, field.field.getName, serializerFactory.getSerializer(field.tpe, context)
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
