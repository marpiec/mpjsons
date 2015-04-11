package pl.mpieciukiewicz.mpjsons.impl.factoryimpl


import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryMemoizer(private val deserializerFactory: DeserializerFactoryImpl) {

  private var getDeserializerCache: Map[Type, JsonTypeDeserializer[_ <: Any]] = Map()

  def registerDeserializer(tpe: Type, deserializer: JsonTypeDeserializer[_ <: Any]) {
    deserializerFactory.registerDeserializer(tpe, deserializer)
  }

  def registerSuperclassDeserializer(tpe: Type, deserializer: JsonTypeDeserializer[_ <: Any]) {
    deserializerFactory.registerSuperclassDeserializer(tpe, deserializer)
  }

  def getDeserializer[T](tpe: Type): JsonTypeDeserializer[T] = {
    getDeserializerCache.getOrElse(tpe, {
      val deserializer = deserializerFactory.getDeserializer(tpe)
      getDeserializerCache += tpe -> deserializer
      deserializer
    }).asInstanceOf[JsonTypeDeserializer[T]]
  }

}
