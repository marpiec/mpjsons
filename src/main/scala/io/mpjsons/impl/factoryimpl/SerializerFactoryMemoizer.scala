package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeSerializer

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer extends SerializerFactoryImpl {

  private var getSerializerCache: Map[Type, JsonTypeSerializer[_]] = Map()

  def getSerializer[T](orgtpe: Type, context: Map[Symbol, Type]): JsonTypeSerializer[T] = {

    val tpe = if(orgtpe.typeSymbol.isParameter) {
      context(orgtpe.typeSymbol)
    } else {
      orgtpe
    }

    val newContext = context ++ tpe.typeSymbol.typeSignature.typeParams.zip(tpe.typeArgs).toMap

    getSerializerCache.getOrElse(tpe, {
      val serializer: JsonTypeSerializer[_] = super.getSerializerNoCache(tpe, newContext)
      getSerializerCache += tpe -> serializer
      serializer
    }).asInstanceOf[JsonTypeSerializer[T]]

  }
}
