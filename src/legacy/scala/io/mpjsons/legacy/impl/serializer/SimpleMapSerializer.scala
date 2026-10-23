package io.mpjsons.legacy.impl.serializer

import io.mpjsons.legacy.{JsonTypeSerializer, NullPolicy}
import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.util.Context

import scala.reflect.runtime.universe._

/**
 * Writes a `Map[String, T]` as a JSON object `{"key":value,...}`.
 *
 * A null value is treated like a null bean field and follows the factory's NullPolicy: `Fail` throws naming the
 * key, `Omit` leaves the entry out, `WriteNull` writes `"key":null`. A null key cannot be an object key and throws.
 * @author Marcin Pieciukiewicz
 */

class SimpleMapSerializer[T](serializerFactory: SerializerFactory, private val context: Context)(implicit tag: TypeTag[T]) extends JsonTypeSerializer[Map[String, T]] {


  private val valueSerializer = serializerFactory.getSerializer[T](tag.tpe, context)
  private val nullPolicy = serializerFactory.config.nullPolicy


  override def serialize(obj: Map[String, T], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('{')

    var isNotFirstField = false

    obj.foreach {
      case (name, value) =>

        if (name == null) {
          throw new IllegalArgumentException("A null map key cannot be written as a JSON object key")
        }

        if (value == null && nullPolicy == NullPolicy.Fail) {
          throw new IllegalArgumentException("Null value is not allowed for map key '" + name +
            "' (NullPolicy.Fail); use NullPolicy.Omit or WriteNull to allow nulls")
        }

        if (value != null || nullPolicy == NullPolicy.WriteNull) {

          if (isNotFirstField) {
            jsonBuilder.append(',')
          } else {
            isNotFirstField = true
          }

          StringSerializer.serialize(name, jsonBuilder)
          jsonBuilder.append(':')
          if (value == null) {
            jsonBuilder.append("null")
          } else {
            valueSerializer.serialize(value, jsonBuilder)
          }
        }
    }

    jsonBuilder.append('}')
  }

}
