package pl.marpiec.mpjsons.impl

import pl.marpiec.mpjsons.impl.serializer._
import pl.marpiec.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object SerializerFactory {

  var additionalSerializers: Map[Class[_], JsonTypeSerializer] = Map[Class[_], JsonTypeSerializer]()
  var additionalSuperclassSerializers: Map[Class[_], JsonTypeSerializer] = Map[Class[_], JsonTypeSerializer]()

  def registerSerializer(clazz: Class[_], serializer: JsonTypeSerializer) {
    additionalSerializers += clazz -> serializer
  }

  def registerSuperclassSerializer(clazz: Class[_], serializer: JsonTypeSerializer) {
    additionalSuperclassSerializers += clazz -> serializer
  }

  def getSerializer(obj: Any): JsonTypeSerializer = {

    if (obj.isInstanceOf[Long]) {
      return SimpleToStringSerializer
    } else if (obj.isInstanceOf[Int]) {
      return SimpleToStringSerializer
    } else if (obj.isInstanceOf[Short]) {
      return SimpleToStringSerializer
    } else if (obj.isInstanceOf[Byte]) {
      return SimpleToStringSerializer
    } else if (obj.isInstanceOf[Boolean]) {
      return SimpleToStringSerializer
    } else if (obj.isInstanceOf[String]) {
      return StringSerializer
    } else if (obj.isInstanceOf[Double]) {
      return SimpleToStringSerializer
    } else if (obj.isInstanceOf[Float]) {
      return SimpleToStringSerializer
    } else if (obj.asInstanceOf[AnyRef].getClass.isArray) {
      return ArraySerializer
    } else if (obj.isInstanceOf[Seq[_]]) {
      return SeqSerializer
    } else if (obj.isInstanceOf[Set[_]]) {
      return SetSerializer
    } else if (obj.isInstanceOf[Tuple2[_, _]]) {
      return Tuple2Serializer
    } else if (obj.isInstanceOf[Option[_]]) {
      return OptionSerializer
    } else if (obj.isInstanceOf[Map[_, _]]) {
      return MapSerializer
    }

    for ((clazzType, serializer) <- additionalSuperclassSerializers) {
      if (clazzType.isInstance(obj)) {
        return serializer
      }
    }

    val serializerOption = additionalSerializers.get(obj.asInstanceOf[AnyRef].getClass)

    if (serializerOption.isDefined) {
      return serializerOption.get
    } else {
      return BeanSerializer
    }

  }
}
