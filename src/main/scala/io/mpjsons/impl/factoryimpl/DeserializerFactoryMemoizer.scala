package io.mpjsons.impl.factoryimpl


import io.mpjsons.JsonTypeDeserializer

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryMemoizer extends DeserializerFactoryImpl {

  private var getDeserializerCache: Map[Type, JsonTypeDeserializer[_ <: Any]] = Map()

  def getDeserializer[T](orgtpe: Type, context: Map[Symbol, Type]): JsonTypeDeserializer[T] = {

    val tpe = if(orgtpe.typeSymbol.isParameter) {
      context(orgtpe.typeSymbol)
    } else {
      orgtpe
    }

    getDeserializerCache.getOrElse(tpe, {
      val deserializer = super.getDeserializerNoCache(tpe, context)
      getDeserializerCache += tpe -> deserializer
      deserializer
    }).asInstanceOf[JsonTypeDeserializer[T]]
  }

}
