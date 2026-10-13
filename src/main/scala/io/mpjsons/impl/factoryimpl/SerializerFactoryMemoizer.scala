package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.util.Context

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer(ignoreNullFields: Boolean) extends SerializerFactoryImpl(ignoreNullFields) {

  private var getSerializerCache: Map[(Type, Boolean), JsonTypeSerializer[_]] = Map.empty

  def getSerializer[T](orgtpe: Type, context: Context, allowSuperType: Boolean = true): JsonTypeSerializer[T] = {

    // Replace every type parameter (e.g. the T in List[T]) with its binding from the context,
    // so that the cache key is always a fully concrete type (List[Int] vs List[String]).
    val tpe = TypeParamsResolver.resolve(orgtpe, context)

    val newContext = Context(tpe :: context.typesStack,
      context.typeParams ++ tpe.typeSymbol.typeSignature.typeParams.zip(tpe.typeArgs).toMap)

    getSerializerCache.getOrElse((tpe, allowSuperType), {
      val serializer: JsonTypeSerializer[_] = super.getSerializerNoCache(tpe, newContext, allowSuperType)
      getSerializerCache += (tpe, allowSuperType) -> serializer
      serializer
    }).asInstanceOf[JsonTypeSerializer[T]]

  }
}
