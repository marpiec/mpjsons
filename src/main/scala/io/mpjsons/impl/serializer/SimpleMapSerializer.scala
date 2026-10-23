package io.mpjsons.impl.serializer

import io.mpjsons.{JsonTypeSerializer, NullPolicy}
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.Context

import scala.reflect.runtime.universe._

/**
 * Writes a `Map[String, T]` as a JSON object `{"key":value,...}`.
 *
 * A null value is treated like a null bean field and follows the factory's NullPolicy: `Fail` throws naming the
 * key, `Omit` leaves the entry out, `WriteNull` writes `"key":null`. A null key cannot be an object key and throws.
 * @author Marcin Pieciukiewicz
 */

class SimpleMapSerializer[T](serializerFactory: SerializerFactory, private val context: Context)(implicit tag: TypeTag[T]) extends JsonTypeSerializer[Map[String, T]] with Utf8Writer[Map[String, T]] {


  private val valueSerializer = serializerFactory.getSerializer[T](tag.tpe, context)
  private val nullPolicy = serializerFactory.config.nullPolicy
  private val valueWriter = Utf8Writer.of(valueSerializer)

  override def writeUtf8(obj: Map[String, T], out: Utf8Output): Unit = {

    out.writeByte('{')

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
            out.writeByte(',')
          } else {
            isNotFirstField = true
          }

          StringSerializer.writeUtf8(name, out)
          out.writeByte(':')
          if (value == null) {
            out.writeAscii("null")
          } else {
            valueWriter.writeUtf8(value, out)
          }
        }
    }

    out.writeByte('}')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[SimpleMapSerializer[_]]


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
