package pl.mpieciukiewicz.mpjsons.impl.factoryimpl


import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryMemoizer(private val deserializerFactory: DeserializerFactoryImpl) {

  private var getDeserializerCache: Map[Class[_], JsonTypeDeserializer[_]] = Map()

  def registerDeserializer(clazz: Class[_], deserializer: JsonTypeDeserializer[_]) {
    deserializerFactory.registerDeserializer(clazz, deserializer)
  }

  def registerSuperclassDeserializer(clazz: Class[_], deserializer: JsonTypeDeserializer[_]) {
    deserializerFactory.registerSuperclassDeserializer(clazz, deserializer)
  }

  def getDeserializer(clazz: Class[_]): JsonTypeDeserializer[_] = {
    getDeserializerCache.get(clazz).getOrElse {
      val deserializer = deserializerFactory.getDeserializer(clazz)
      getDeserializerCache += clazz -> deserializer
      deserializer
    }
  }

}
