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

    val newContext = context ++ tpe.typeSymbol.typeSignature.typeParams.zip(tpe.typeArgs).toMap

    getDeserializerCache.getOrElse(tpe, {
      val deserializer = super.getDeserializerNoCache(tpe, newContext)
      getDeserializerCache += tpe -> deserializer
      deserializer
    }).asInstanceOf[JsonTypeDeserializer[T]]
  }

}
