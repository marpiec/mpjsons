package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{Context, TypesUtil}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class MapSerializer[K, V](serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends JsonTypeSerializer[scala.collection.Map[K, V]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  val keySerializer = serializerFactory.getSerializer(subtypes._1, context).asInstanceOf[JsonTypeSerializer[K]]
  val valueSerializer = serializerFactory.getSerializer(subtypes._2, context).asInstanceOf[JsonTypeSerializer[V]]


  override def serialize(map: scala.collection.Map[K, V], jsonBuilder: StringBuilder) = {


    jsonBuilder.append('[')
    var nonFirstField = false

    for ((key: K, value: V) <- map) {
      if (nonFirstField) {
        jsonBuilder.append(',')
      } else {
        nonFirstField = true
      }
      jsonBuilder.append('[')

      keySerializer.serialize(key, jsonBuilder)

      jsonBuilder.append(',')

      valueSerializer.serialize(value, jsonBuilder)

      jsonBuilder.append(']')
    }

    jsonBuilder.append(']')
  }

}
