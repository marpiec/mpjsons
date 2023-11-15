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

class SimpleMapSerializer[T](serializerFactory: SerializerFactory, private val context: Context)(implicit tag: TypeTag[T]) extends JsonTypeSerializer[Map[String, T]] {


  private val valueSerializer = serializerFactory.getSerializer[T](tag.tpe, context)


  override def serialize(obj: Map[String, T], jsonBuilder: StringBuilder): Unit = {


    jsonBuilder.append('{')

    var isNotFirstField = false


    obj.foreach {
      case (name, value) =>

        if (value == null) {
          // skip null values
        } else {

          if (isNotFirstField) {
            jsonBuilder.append(',')
          } else {
            isNotFirstField = true
          }

          jsonBuilder.append('"').append(name).append('"').append(':')
          valueSerializer.serialize(value, jsonBuilder)
        }
    }

    jsonBuilder.append('}')
  }

}
