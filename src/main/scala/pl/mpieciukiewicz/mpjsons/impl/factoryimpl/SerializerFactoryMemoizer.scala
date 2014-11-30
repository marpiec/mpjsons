package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryMemoizer(private val serializerFactory: SerializerFactoryImpl) {

  private var getSerializerCache: Map[Class[_], JsonTypeSerializer] = Map()

  def registerSerializer(clazz: Class[_], serializer: JsonTypeSerializer) {
    serializerFactory.registerSerializer(clazz, serializer)
  }

  def registerSuperclassSerializer(clazz: Class[_], serializer: JsonTypeSerializer) {
    serializerFactory.registerSuperclassSerializer(clazz, serializer)
  }

  def getSerializer(clazz: Class[_]): JsonTypeSerializer = {
    getSerializerCache.get(clazz).getOrElse {
      val serializer: JsonTypeSerializer = serializerFactory.getSerializer(clazz)
      getSerializerCache += clazz -> serializer
      serializer
    }

  }
}
