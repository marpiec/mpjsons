package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer(private val serializerFactory: SerializerFactoryImpl) {

  private var getSerializerCache: Map[Type, JsonTypeSerializer[_]] = Map()

  def registerSerializer(tpe: Type, serializer: JsonTypeSerializer[_]) {
    serializerFactory.registerSerializer(tpe, serializer)
  }

  def registerSuperclassSerializer(tpe: Type, serializer: JsonTypeSerializer[_]) {
    serializerFactory.registerSuperclassSerializer(tpe, serializer)
  }

  def getSerializer(clazz: Class[_]): JsonTypeSerializer[_] = {
    null
  }

  def getSerializer(tpe: Type): JsonTypeSerializer[_] = {
    getSerializerCache.getOrElse(tpe, {
      val serializer: JsonTypeSerializer[_] = serializerFactory.getSerializer(tpe)
      getSerializerCache += tpe -> serializer
      serializer
    })

  }
}
