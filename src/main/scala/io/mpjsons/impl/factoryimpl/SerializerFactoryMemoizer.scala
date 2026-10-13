package io.mpjsons.impl.factoryimpl

import io.mpjsons.{JsonTypeSerializer, MPJsonsConfig}
import io.mpjsons.impl.util.Context

import java.util.concurrent.ConcurrentHashMap
import scala.reflect.runtime.universe._

/**
 * Caching layer: one serializer instance per fully resolved type.
 * The lookup is done before any Context is built, so a cache hit (the per-call case for root types)
 * costs one hash lookup and no allocation. Concurrent first-time use may build a serializer twice;
 * the first one stored wins and both are equivalent.
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer(config: MPJsonsConfig) extends SerializerFactoryImpl(config) {

  private val cache = new ConcurrentHashMap[Type, JsonTypeSerializer[_]]()
  private val cacheWithoutSuperTypes = new ConcurrentHashMap[Type, JsonTypeSerializer[_]]()

  def getSerializer[T](orgtpe: Type, context: Context, allowSuperType: Boolean = true): JsonTypeSerializer[T] = {

    // Replace every type parameter (e.g. the T in List[T]) with its binding from the context,
    // so that the cache key is always a fully concrete type (List[Int] vs List[String]).
    val tpe = TypeParamsResolver.resolve(orgtpe, context)

    val map = if (allowSuperType) cache else cacheWithoutSuperTypes
    val cached = map.get(tpe)
    if (cached != null) {
      cached.asInstanceOf[JsonTypeSerializer[T]]
    } else {
      val newContext = Context(tpe :: context.typesStack,
        context.typeParams ++ tpe.typeSymbol.typeSignature.typeParams.zip(tpe.typeArgs).toMap)
      val created = super.getSerializerNoCache(tpe, newContext, allowSuperType)
      val previous = map.putIfAbsent(tpe, created)
      (if (previous == null) created else previous).asInstanceOf[JsonTypeSerializer[T]]
    }
  }
}
