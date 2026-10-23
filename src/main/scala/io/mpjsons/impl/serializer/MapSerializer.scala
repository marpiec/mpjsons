package io.mpjsons.impl.serializer

import io.mpjsons.{JsonTypeSerializer, NullPolicy}
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * Historical form: an array of [key,value] pairs, which works for any key type.
 * Object form (`mapsAsObjects`, only for scalar key types, see MapKeys): {"key":value,...} with the key's text
 * as the JSON key.
 * Pair form: null keys and values are elements, written as `null` when the NullPolicy allows nulls and rejected
 * under Fail. Object form: a null value is treated like a null bean field (Fail throws naming the key, Omit leaves
 * the entry out, WriteNull writes `"key":null`); a null key has no object form and always fails.
 * @author Marcin Pieciukiewicz
 */

class MapSerializer[K, V](serializerFactory: SerializerFactory, tpe: Type, context: Context, asObject: Boolean)
  extends JsonTypeSerializer[scala.collection.Map[K, V]] with Utf8Writer[scala.collection.Map[K, V]] {

  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context) = this(serializerFactory, tpe, context, false)

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val keySerializer = serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[K]]
  private val valueSerializer = serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[V]]
  private val keyIsString = subtypes._1 <:< MapSerializer.stringType
  private val nullPolicy = serializerFactory.config.nullPolicy
  private val nullsAllowed = nullPolicy != NullPolicy.Fail


  override def serialize(map: scala.collection.Map[K, V], jsonBuilder: StringBuilder): Unit = {
    if (asObject) serializeAsObject(map, jsonBuilder) else serializeAsPairs(map, jsonBuilder)
  }

  private def serializeAsPairs(map: scala.collection.Map[K, V], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('[')
    var nonFirstField = false

    map.foreachEntry { (key, value) =>
      if (nonFirstField) {
        jsonBuilder.append(',')
      } else {
        nonFirstField = true
      }
      jsonBuilder.append('[')

      if (key == null) NullValues.writeNull(jsonBuilder, nullsAllowed, "a key of " + tpe) else keySerializer.serialize(key, jsonBuilder)

      jsonBuilder.append(',')

      if (value == null) NullValues.writeNull(jsonBuilder, nullsAllowed, "a value of " + tpe) else valueSerializer.serialize(value, jsonBuilder)

      jsonBuilder.append(']')
    }

    jsonBuilder.append(']')
  }

  /** Object form: a null value is treated like a null bean field (Fail throws, Omit skips the entry, WriteNull writes null). */
  private def serializeAsObject(map: scala.collection.Map[K, V], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('{')
    var nonFirstField = false

    map.foreachEntry { (key, value) =>
      if (key == null) {
        throw new IllegalArgumentException("A null map key cannot be written as a JSON object key")
      }

      if (value == null && nullPolicy == NullPolicy.Fail) {
        throw new IllegalArgumentException("Null value is not allowed for map key '" + key + "' in " + tpe +
          " (NullPolicy.Fail); use NullPolicy.Omit or WriteNull to allow nulls")
      }

      if (value != null || nullPolicy == NullPolicy.WriteNull) {
        if (nonFirstField) {
          jsonBuilder.append(',')
        } else {
          nonFirstField = true
        }

        if (keyIsString) {
          keySerializer.serialize(key, jsonBuilder)
        } else {
          jsonBuilder.append('"').append(key.toString).append('"')
        }

        jsonBuilder.append(':')

        if (value == null) jsonBuilder.append("null") else valueSerializer.serialize(value, jsonBuilder)
      }
    }

    jsonBuilder.append('}')
  }

  private val keyWriter = Utf8Writer.of(keySerializer)
  private val valueWriter = Utf8Writer.of(valueSerializer)

  override def writeUtf8(map: scala.collection.Map[K, V], out: Utf8Output): Unit = {
    if (asObject) writeAsObject(map, out) else writeAsPairs(map, out)
  }

  private def writeAsPairs(map: scala.collection.Map[K, V], out: Utf8Output): Unit = {

    out.writeByte('[')
    var nonFirstField = false

    map.foreachEntry { (key, value) =>
      if (nonFirstField) {
        out.writeByte(',')
      } else {
        nonFirstField = true
      }
      out.writeByte('[')

      if (key == null) NullValues.writeNull(out, nullsAllowed, "a key of " + tpe) else keyWriter.writeUtf8(key, out)

      out.writeByte(',')

      if (value == null) NullValues.writeNull(out, nullsAllowed, "a value of " + tpe) else valueWriter.writeUtf8(value, out)

      out.writeByte(']')
    }

    out.writeByte(']')
  }

  private def writeAsObject(map: scala.collection.Map[K, V], out: Utf8Output): Unit = {

    out.writeByte('{')
    var nonFirstField = false

    map.foreachEntry { (key, value) =>
      if (key == null) {
        throw new IllegalArgumentException("A null map key cannot be written as a JSON object key")
      }

      if (value == null && nullPolicy == NullPolicy.Fail) {
        throw new IllegalArgumentException("Null value is not allowed for map key '" + key + "' in " + tpe +
          " (NullPolicy.Fail); use NullPolicy.Omit or WriteNull to allow nulls")
      }

      if (value != null || nullPolicy == NullPolicy.WriteNull) {
        if (nonFirstField) {
          out.writeByte(',')
        } else {
          nonFirstField = true
        }

        if (keyIsString) {
          keyWriter.writeUtf8(key, out)
        } else {
          out.writeByte('"')
          out.writeText(key.toString)
          out.writeByte('"')
        }

        out.writeByte(':')

        if (value == null) out.writeAscii("null") else valueWriter.writeUtf8(value, out)
      }
    }

    out.writeByte('}')
  }

  override private[mpjsons] def writesUtf8Itself: Boolean = getClass eq classOf[MapSerializer[_, _]]

}

object MapSerializer {
  private val stringType = typeOf[String]
}
