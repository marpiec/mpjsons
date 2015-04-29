package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeSerializer

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer extends SerializerFactoryImpl {

  private var getSerializerCache: Map[Type, JsonTypeSerializer[_]] = Map()

  def getSerializer[T](orgtpe: Type, context: Context): JsonTypeSerializer[T] = {

    val tpe = if(orgtpe.typeSymbol.isParameter) {
      context.typeParams(orgtpe.typeSymbol)
    } else {
      orgtpe
    }

    val newContext = Context(tpe :: context.typesStack,
      context.typeParams ++ tpe.typeSymbol.typeSignature.typeParams.zip(tpe.typeArgs).toMap)

    getSerializerCache.getOrElse(tpe, {
      val serializer: JsonTypeSerializer[_] = super.getSerializerNoCache(tpe, newContext)
      getSerializerCache += tpe -> serializer
      serializer
    }).asInstanceOf[JsonTypeSerializer[T]]

  }
}
