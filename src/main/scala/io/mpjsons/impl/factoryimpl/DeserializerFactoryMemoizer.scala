package io.mpjsons.impl.factoryimpl


import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.Context

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryMemoizer(ignoreNonExistingFields: Boolean) extends DeserializerFactoryImpl(ignoreNonExistingFields) {

  private var getDeserializerCache: Map[Type, JsonTypeDeserializer[_ <: Any]] = Map()

  def getDeserializer[T](orgtpe: Type, context: Context): JsonTypeDeserializer[T] = {

    val tpe = if (orgtpe.typeSymbol.isParameter) {
      context.typeParams(orgtpe.typeSymbol)
    } else {
      orgtpe
    }

    val newContext = Context(tpe :: context.typesStack,
      context.typeParams ++ tpe.typeSymbol.typeSignature.typeParams.zip(tpe.typeArgs).toMap)

    getDeserializerCache.getOrElse(tpe, {
      val deserializer = super.getDeserializerNoCache(tpe, newContext)
      getDeserializerCache += tpe -> deserializer
      deserializer
    }).asInstanceOf[JsonTypeDeserializer[T]]
  }

}
