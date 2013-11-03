package pl.marpiec.mpjsons.impl.factoryimpl

import pl.marpiec.mpjsons.impl.serializer._
import pl.marpiec.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryImpl {

  private var additionalSerializers: Map[Class[_], JsonTypeSerializer] = Map[Class[_], JsonTypeSerializer]()
  private var additionalSuperclassSerializers: Map[Class[_], JsonTypeSerializer] = Map[Class[_], JsonTypeSerializer]()

  def registerSerializer(clazz: Class[_], serializer: JsonTypeSerializer) {
    additionalSerializers += clazz -> serializer
  }

  def registerSuperclassSerializer(clazz: Class[_], serializer: JsonTypeSerializer) {
    additionalSuperclassSerializers += clazz -> serializer
  }

  def getSerializer(clazz: Class[_]): JsonTypeSerializer = {

    if (classOf[Long] == clazz || classOf[java.lang.Long] == clazz) {
      return SimpleNakedStringSerializer
    } else if (classOf[Int] == clazz || classOf[java.lang.Integer] == clazz) {
      return SimpleNakedStringSerializer
    } else if (classOf[Short] == clazz || classOf[java.lang.Short] == clazz) {
      return SimpleNakedStringSerializer
    } else if (classOf[Byte] == clazz || classOf[java.lang.Byte] == clazz) {
      return SimpleNakedStringSerializer
    } else if (classOf[Boolean] == clazz || classOf[java.lang.Boolean] == clazz) {
      return SimpleNakedStringSerializer
    } else if (classOf[Char] == clazz || classOf[java.lang.Character] == clazz) {
      return SimpleStringSerializer
    } else if (classOf[Double] == clazz || classOf[java.lang.Double] == clazz) {
      return SimpleNakedStringSerializer
    } else if (classOf[Float] == clazz || classOf[java.lang.Float] == clazz) {
      return SimpleNakedStringSerializer
    } else if (classOf[String] == clazz) {
      return StringSerializer
    } else if (clazz.isArray) {
      return ArraySerializer
    } else if (classOf[Seq[_]].isAssignableFrom(clazz)) {
      return SeqSerializer
    } else if (classOf[Set[_]].isAssignableFrom(clazz)) {
      return SetSerializer
    } else if (classOf[(_,_)].isAssignableFrom(clazz)) {
      return Tuple2Serializer
    } else if (classOf[Option[_]].isAssignableFrom(clazz)) {
      return OptionSerializer
    } else if (classOf[Map[_,_]].isAssignableFrom(clazz)) {
      return MapSerializer
    }

    for ((clazzType, serializer) <- additionalSuperclassSerializers) {
      if (clazzType.isAssignableFrom(clazz)) {
        return serializer
      }
    }

    additionalSerializers.get(clazz).getOrElse(
      return BeanSerializer
    )

  }
}
