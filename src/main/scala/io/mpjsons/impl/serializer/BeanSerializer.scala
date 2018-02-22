package io.mpjsons.impl.serializer

import java.lang.reflect.Field

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.util.reflection.ReflectionUtil

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

case class FieldInfo[T](field: Field, name: String, jsonTypeSerializer: JsonTypeSerializer[T], nullable: Boolean)

class BeanSerializer(serializerFactory: SerializerFactory, private val tpe: Type, private val context: Context) extends JsonTypeSerializer[AnyRef] {

  private val fields = ReflectionUtil.getAllAccessibleFields(tpe)
  /** Lazy val to prevent StackOverflow while construction recursive type serializer */
  private lazy val fieldsWithSerializers = fields.map { field =>
    FieldInfo(field.field, field.field.getName, serializerFactory.getSerializer(field.tpe, context)
      .asInstanceOf[JsonTypeSerializer[AnyRef]], field.nullable)
  }


  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder) = {


    jsonBuilder.append('{')

    var isNotFirstField = false

    for (field <- fieldsWithSerializers) {

      val value = field.field.get(obj)
      if (value == null) {
        if (!field.nullable) {
          throw new IllegalArgumentException("Null value is not allowed for field " + field.name +" in type " + tpe.typeSymbol.fullName)
        }
      } else {

        if (isNotFirstField) {
          jsonBuilder.append(',')
        } else {
          isNotFirstField = true
        }

        jsonBuilder.append('"').append(field.name).append('"').append(':')
        field.jsonTypeSerializer.serialize(value, jsonBuilder)
      }
    }

    jsonBuilder.append('}')
  }

}
