package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeSerializer

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer(private val serializerFactory: SerializerFactoryImpl) {

  private var getSerializerCache: Map[Type, JsonTypeSerializer[_]] = Map()

  def registerSerializer[T](tpe: Type, serializer: JsonTypeSerializer[T]) {
    serializerFactory.registerSerializer(tpe, serializer)
  }

  def registerSuperclassSerializer(tpe: Type, serializer: JsonTypeSerializer[_]) {
    serializerFactory.registerSuperclassSerializer(tpe, serializer)
  }


  def getSerializer[T](tpe: Type): JsonTypeSerializer[T] = {
    getSerializerCache.getOrElse(tpe, {
      val serializer: JsonTypeSerializer[_] = serializerFactory.getSerializer(tpe)
      getSerializerCache += tpe -> serializer
      serializer
    }).asInstanceOf[JsonTypeSerializer[T]]

  }
}
