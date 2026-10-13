package io.mpjsons.impl.serializer

import io.mpjsons.{JsonTypeSerializer, NullPolicy}
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * Historical form: an array of [key,value] pairs, which works for any key type.
 * Object form (`mapsAsObjects`, only for scalar key types, see MapKeys): {"key":value,...} with the key's text
 * as the JSON key. Null keys and values are written as `null` in the pair form when the NullPolicy allows nulls
 * and rejected under Fail; a null key has no object form and always fails.
 * @author Marcin Pieciukiewicz
 */

class MapSerializer[K, V](serializerFactory: SerializerFactory, tpe: Type, context: Context, asObject: Boolean)
  extends JsonTypeSerializer[scala.collection.Map[K, V]] {

  def this(serializerFactory: SerializerFactory, tpe: Type, context: Context) = this(serializerFactory, tpe, context, false)

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val keySerializer = serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[K]]
  private val valueSerializer = serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[V]]
  private val keyIsString = subtypes._1 <:< MapSerializer.stringType
  private val nullsAllowed = serializerFactory.config.nullPolicy != NullPolicy.Fail


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

  private def serializeAsObject(map: scala.collection.Map[K, V], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('{')
    var nonFirstField = false

    map.foreachEntry { (key, value) =>
      if (nonFirstField) {
        jsonBuilder.append(',')
      } else {
        nonFirstField = true
      }

      if (key == null) {
        throw new IllegalArgumentException("A null map key cannot be written as a JSON object key")
      } else if (keyIsString) {
        keySerializer.serialize(key, jsonBuilder)
      } else {
        jsonBuilder.append('"').append(key.toString).append('"')
      }

      jsonBuilder.append(':')

      if (value == null) NullValues.writeNull(jsonBuilder, nullsAllowed, "a value of " + tpe) else valueSerializer.serialize(value, jsonBuilder)
    }

    jsonBuilder.append('}')
  }

}

object MapSerializer {
  private val stringType = typeOf[String]
}
