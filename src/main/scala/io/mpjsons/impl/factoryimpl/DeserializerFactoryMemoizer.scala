package io.mpjsons.impl.factoryimpl


import io.mpjsons.JsonTypeDeserializer

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryMemoizer extends DeserializerFactoryImpl {

  private var getDeserializerCache: Map[Type, JsonTypeDeserializer[_ <: Any]] = Map()

  def getDeserializer[T](tpe: Type): JsonTypeDeserializer[T] = {
    getDeserializerCache.getOrElse(tpe, {
      val deserializer = super.getDeserializerNoCache(tpe)
      getDeserializerCache += tpe -> deserializer
      deserializer
    }).asInstanceOf[JsonTypeDeserializer[T]]
  }

}
