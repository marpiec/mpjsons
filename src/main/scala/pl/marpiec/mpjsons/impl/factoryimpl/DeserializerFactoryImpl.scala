package pl.marpiec.mpjsons.impl.factoryimpl

import collection.mutable.ListBuffer

import pl.marpiec.mpjsons.impl.deserializer.primitives._
import pl.marpiec.mpjsons.impl.deserializer._
import pl.marpiec.mpjsons.JsonTypeDeserializer
import pl.marpiec.mpjsons.impl.deserializer.array.{SetDeserializer, OptionDeserializer, ListDeserializer, ArrayDeserializer}

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryImpl {

  private var additionalDeserializers: Map[Class[_], JsonTypeDeserializer[_]] = Map[Class[_], JsonTypeDeserializer[_]]()
  private var additionalSuperclassDeserializers: Map[Class[_], JsonTypeDeserializer[_]] = Map[Class[_], JsonTypeDeserializer[_]]()

  def registerDeserializer(clazz: Class[_], deserializer: JsonTypeDeserializer[_]) {
    additionalDeserializers += clazz -> deserializer
  }

  def registerSuperclassDeserializer(clazz: Class[_], deserializer: JsonTypeDeserializer[_]) {
    additionalSuperclassDeserializers += clazz -> deserializer
  }

  def getDeserializer(clazz: Class[_]): JsonTypeDeserializer[_] = {
    if (clazz.equals(classOf[Long])) {
      return LongDeserializer
    } else if (clazz.equals(classOf[Int])) {
      return IntDeserializer
    } else if (clazz.equals(classOf[Boolean])) {
      return BooleanDeserializer
    } else if (clazz.equals(classOf[String])) {
      return StringDeserializer
    } else if (clazz.equals(classOf[Double])) {
      return DoubleDeserializer
    } else if (clazz.equals(classOf[Float])) {
      return FloatDeserializer
    } else if (clazz.equals(classOf[Short])) {
      return ShortDeserializer
    } else if (clazz.equals(classOf[Byte])) {
      return ByteDeserializer
    } else if (clazz.equals(classOf[Char])) {
      return CharDeserializer
    } else if (clazz.isArray) {
      return ArrayDeserializer
    } else if (clazz.equals(classOf[List[_]])) {
      return ListDeserializer
    } else if (clazz.equals(classOf[Set[_]])) {
      return SetDeserializer
    } else if (clazz.equals(classOf[(_, _)])) {
      return Tuple2Deserializer
    } else if (clazz.equals(classOf[Option[_]])) {
      return OptionDeserializer
    } else if (clazz.equals(classOf[Map[_, _]])) {
      return MapDeserializer
    }

    for ((clazzType, deserializer) <- additionalSuperclassDeserializers) {
      if (clazzType.isAssignableFrom(clazz)) {
        return deserializer
      }
    }

    if (clazz.equals(classOf[ListBuffer[_]])) {
      throw new IllegalArgumentException("ListBuffer is not supported, use immutable List instead")
    }

    return additionalDeserializers.get(clazz).getOrElse(BeanDeserializer)
  }

}