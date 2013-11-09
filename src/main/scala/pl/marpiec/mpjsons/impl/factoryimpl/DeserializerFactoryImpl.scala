package pl.marpiec.mpjsons.impl.factoryimpl

import collection.mutable.ListBuffer

import pl.marpiec.mpjsons.impl.deserializer.primitives._
import pl.marpiec.mpjsons.impl.deserializer._
import pl.marpiec.mpjsons.JsonTypeDeserializer
import pl.marpiec.mpjsons.impl.deserializer.array._
import scala.collection.immutable.{Stack, Queue}

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
    if (clazz == classOf[Long]) {
      return LongDeserializer
    } else if (clazz == classOf[Int]) {
      return IntDeserializer
    } else if (clazz == classOf[Boolean]) {
      return BooleanDeserializer
    } else if (clazz == classOf[String]) {
      return StringDeserializer
    } else if (clazz == classOf[Double]) {
      return DoubleDeserializer
    } else if (clazz == classOf[Float]) {
      return FloatDeserializer
    } else if (clazz == classOf[Short]) {
      return ShortDeserializer
    } else if (clazz == classOf[Byte]) {
      return ByteDeserializer
    } else if (clazz == classOf[Char]) {
      return CharDeserializer
    } else if (clazz.isArray) {
      return ArrayDeserializer
    } else if (clazz == classOf[List[_]]) {
      return ListDeserializer
    } else if (clazz == classOf[Vector[_]]) {
      return VectorDeserializer
    } else if (clazz == classOf[Stream[_]]) {
      return StreamDeserializer
    } else if (clazz == classOf[Queue[_]]) {
      return ImmutableQueueDeserializer
    } else if (clazz == classOf[Stack[_]]) {
      return ImmutableStackDeserializer
    } else if (clazz == classOf[Set[_]]) {
      return SetDeserializer
    } else if (clazz == classOf[(_, _)]) {
      return Tuple2Deserializer
    } else if (clazz == classOf[Option[_]]) {
      return OptionDeserializer
    } else if (clazz == classOf[Map[_, _]]) {
      return MapDeserializer
    }

    for ((clazzType, deserializer) <- additionalSuperclassDeserializers) {
      if (clazzType.isAssignableFrom(clazz)) {
        return deserializer
      }
    }

    if (clazz == classOf[ListBuffer[_]]) {
      throw new IllegalArgumentException("ListBuffer is not supported, use immutable List instead")
    }

    return additionalDeserializers.get(clazz).getOrElse(BeanDeserializer)
  }

}
