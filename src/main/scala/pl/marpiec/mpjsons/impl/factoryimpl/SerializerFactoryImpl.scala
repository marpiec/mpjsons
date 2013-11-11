package pl.marpiec.mpjsons.impl.factoryimpl

import pl.marpiec.mpjsons.impl.serializer._
import pl.marpiec.mpjsons.JsonTypeSerializer
import scala.collection.immutable._
import scala.collection.mutable
import scala.Vector
import scala.Stream
import scala.List

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


    // Primitives, by toString (except Char)
    if (classOf[Long] == clazz || classOf[java.lang.Long] == clazz) {
      return SimpleToStringSerializer
    } else if (classOf[Int] == clazz || classOf[java.lang.Integer] == clazz) {
      return SimpleToStringSerializer
    } else if (classOf[Short] == clazz || classOf[java.lang.Short] == clazz) {
      return SimpleToStringSerializer
    } else if (classOf[Byte] == clazz || classOf[java.lang.Byte] == clazz) {
      return SimpleToStringSerializer
    } else if (classOf[Boolean] == clazz || classOf[java.lang.Boolean] == clazz) {
      return SimpleToStringSerializer
    } else if (classOf[Double] == clazz || classOf[java.lang.Double] == clazz) {
      return SimpleToStringSerializer
    } else if (classOf[Float] == clazz || classOf[java.lang.Float] == clazz) {
      return SimpleToStringSerializer
    }

    // String, StringBuilder, Char
    if (classOf[String] == clazz ||
        classOf[mutable.StringBuilder] == clazz ||
        classOf[Char] == clazz || classOf[java.lang.Character] == clazz) {
      return StringSerializer
    }

    // Arrays
    if (clazz.isArray) {
      return ArraySerializer
    }


    // We don't want to support user's custom collections implicitly,
    // because there will be problem with deserialization
    if(clazz.getName.startsWith("scala.")) {
      //Every immutable collection
      if (classOf[Seq[_]].isAssignableFrom(clazz)) {
        return IterableSerializer
      } else if (classOf[Set[_]].isAssignableFrom(clazz)) {
        return IterableSerializer
      } else if (classOf[Map[_,_]].isAssignableFrom(clazz)) {
        return MapSerializer
      } else if (classOf[Iterable[_]].isAssignableFrom(clazz)) {
        return IterableSerializer
      }

      //Every mutable collection
      if (classOf[mutable.Seq[_]].isAssignableFrom(clazz)) {
        return IterableSerializer
      } else if (classOf[mutable.Set[_]].isAssignableFrom(clazz)) {
        return IterableSerializer
      } else if (classOf[mutable.Map[_,_]].isAssignableFrom(clazz)) {
        return MapSerializer
      } else if (classOf[mutable.Iterable[_]].isAssignableFrom(clazz)) {
        return IterableSerializer
      }

      // Every Tuple, Option
      if (classOf[Product].isAssignableFrom(clazz)) {
        return ProductSerializer
      }

    }


    for ((clazzType, serializer) <- additionalSuperclassSerializers) {
      if (clazzType.isAssignableFrom(clazz)) {
        return serializer
      }
    }

    return additionalSerializers.get(clazz).getOrElse(BeanSerializer)


    //TODO Range, NumericRange
  }


  private def matches(clazz: Class[_], clazzes: List[Class[_]]) = clazzes.contains(clazz)
}
