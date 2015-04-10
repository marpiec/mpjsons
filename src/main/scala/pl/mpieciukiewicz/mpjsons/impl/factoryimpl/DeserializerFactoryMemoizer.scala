package pl.mpieciukiewicz.mpjsons.impl.factoryimpl


import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryMemoizer(private val deserializerFactory: DeserializerFactoryImpl) {

  private var getDeserializerCache: Map[Class[_], JsonTypeDeserializer[_ <: Any]] = Map()

  def registerDeserializer(clazz: Class[_], deserializer: JsonTypeDeserializer[_ <: Any]) {
    deserializerFactory.registerDeserializer(clazz, deserializer)
  }

  def registerSuperclassDeserializer(clazz: Class[_], deserializer: JsonTypeDeserializer[_ <: Any]) {
    deserializerFactory.registerSuperclassDeserializer(clazz, deserializer)
  }

  def getDeserializer[T](clazz: Class[T]): JsonTypeDeserializer[T] = {
    getDeserializerCache.getOrElse(clazz, {
      val deserializer = deserializerFactory.getDeserializer(clazz)
      getDeserializerCache += clazz -> deserializer
      deserializer
    }).asInstanceOf[JsonTypeDeserializer[T]]
  }

}
