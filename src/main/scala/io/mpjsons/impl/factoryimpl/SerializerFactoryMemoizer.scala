package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.util.Context

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer extends SerializerFactoryImpl {

  private var getSerializerCache: Map[(Type, Boolean), JsonTypeSerializer[_]] = Map.empty

  def getSerializer[T](orgtpe: Type, context: Context, allowSuperType: Boolean = true): JsonTypeSerializer[T] = {

    val tpe = if (orgtpe.typeSymbol.isParameter) {
      context.typeParams(orgtpe.typeSymbol)
    } else {
      orgtpe
    }

    val newContext = Context(tpe :: context.typesStack,
      context.typeParams ++ tpe.typeSymbol.typeSignature.typeParams.zip(tpe.typeArgs).toMap)

    getSerializerCache.getOrElse((tpe, allowSuperType), {
      val serializer: JsonTypeSerializer[_] = super.getSerializerNoCache(tpe, newContext, allowSuperType)
      getSerializerCache += (tpe, allowSuperType) -> serializer
      serializer
    }).asInstanceOf[JsonTypeSerializer[T]]

  }
}
