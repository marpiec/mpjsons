package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeSerializer

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer extends SerializerFactoryImpl {

  private var getSerializerCache: Map[Type, JsonTypeSerializer[_]] = Map()

  def getSerializer[T](tpe: Type): JsonTypeSerializer[T] = {
    getSerializerCache.getOrElse(tpe, {
      val serializer: JsonTypeSerializer[_] = super.getSerializerNoCache(tpe)
      getSerializerCache += tpe -> serializer
      serializer
    }).asInstanceOf[JsonTypeSerializer[T]]

  }
}
