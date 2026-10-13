package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * Null keys and values are written as the `null` literal.
 * @author Marcin Pieciukiewicz
 */

class MapSerializer[K, V](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.collection.Map[K, V]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  private val keySerializer = serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[K]]
  private val valueSerializer = serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[V]]


  override def serialize(map: scala.collection.Map[K, V], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('[')
    var nonFirstField = false

    map.foreachEntry { (key, value) =>
      if (nonFirstField) {
        jsonBuilder.append(',')
      } else {
        nonFirstField = true
      }
      jsonBuilder.append('[')

      if (key == null) jsonBuilder.append("null") else keySerializer.serialize(key, jsonBuilder)

      jsonBuilder.append(',')

      if (value == null) jsonBuilder.append("null") else valueSerializer.serialize(value, jsonBuilder)

      jsonBuilder.append(']')
    }

    jsonBuilder.append(']')
  }

}
