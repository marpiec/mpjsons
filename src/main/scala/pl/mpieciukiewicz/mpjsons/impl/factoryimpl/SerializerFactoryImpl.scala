package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.impl.serializer._
import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil
import scala.collection.immutable._
import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryImpl {

  private var additionalSerializers = Map[Type, JsonTypeSerializer[_]]()
  private var additionalSuperclassSerializers = Map[Type, JsonTypeSerializer[_]]()

  def registerSerializer(tpe: Type, serializer: JsonTypeSerializer[_]) {
    additionalSerializers += tpe -> serializer
  }

  def registerSuperclassSerializer(tpe: Type, serializer: JsonTypeSerializer[_]) {
    additionalSuperclassSerializers += tpe -> serializer
  }

  def getSerializer(tpe: Type): JsonTypeSerializer[_] = {


//    // Primitives, by toString (except Char)
//    if (classOf[Long] == tpe || classOf[java.lang.Long] == tpe) {
//      return SimpleToStringSerializer
//    } else if (classOf[Int] == tpe || classOf[java.lang.Integer] == tpe) {
//      return SimpleToStringSerializer
//    } else if (classOf[Short] == tpe || classOf[java.lang.Short] == tpe) {
//      return SimpleToStringSerializer
//    } else if (classOf[Byte] == tpe || classOf[java.lang.Byte] == tpe) {
//      return SimpleToStringSerializer
//    } else if (classOf[Boolean] == tpe || classOf[java.lang.Boolean] == tpe) {
//      return SimpleToStringSerializer
//    } else if (classOf[Double] == tpe || classOf[java.lang.Double] == tpe) {
//      return SimpleToStringSerializer
//    } else if (classOf[Float] == tpe || classOf[java.lang.Float] == tpe) {
//      return SimpleToStringSerializer
//    }
//
//    // String, StringBuilder, Char
//    if (classOf[String] == tpe ||
//        classOf[mutable.StringBuilder] == tpe ||
//        classOf[Char] == tpe || classOf[java.lang.Character] == tpe) {
//      return StringSerializer
//    }
//
//    // Arrays
//    if (tpe.isArray) {
//      return ArraySerializer
//    }
//
//
//    // We don't want to support user's custom collections implicitly,
//    // because there will be problem with deserialization
//    if(tpe.getName.startsWith("scala.")) {
//      //Every immutable collection
//      if (classOf[Seq[_]].isAssignableFrom(tpe)) {
//        return IterableSerializer
//      } else if (classOf[Set[_]].isAssignableFrom(tpe)) {
//        return IterableSerializer
//      } else if (classOf[Map[_,_]].isAssignableFrom(tpe)) {
//        return MapSerializer
//      } else if (classOf[Iterable[_]].isAssignableFrom(tpe)) {
//        return IterableSerializer
//      }
//
//      //Every mutable collection
//      if (classOf[mutable.Seq[_]].isAssignableFrom(tpe)) {
//        return IterableSerializer
//      } else if (classOf[mutable.Set[_]].isAssignableFrom(tpe)) {
//        return IterableSerializer
//      } else if (classOf[mutable.Map[_,_]].isAssignableFrom(tpe)) {
//        return MapSerializer
//      } else if (classOf[mutable.Iterable[_]].isAssignableFrom(tpe)) {
//        return IterableSerializer
//      }
//
//      // Every Tuple, Option
//      if (classOf[Product].isAssignableFrom(tpe)) {
//        return ProductSerializer
//      }
//
//    }
//
//
//    for ((tpeType, serializer) <- additionalSuperclassSerializers) {
//      if (tpeType.isAssignableFrom(tpe)) {
//        return serializer
//      }
//    }
//
//    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.getName == "MODULE$")) {
//        return SingletonObjectSerializer
//    }
//
//    val additionalSerializerOption = additionalSerializers.get(tpe)
//
//    if(additionalSerializerOption.isDefined) {
//      additionalSerializerOption.get
//    } else if (classOf[Type].isAssignableFrom(tpe)) {
//      throw new IllegalArgumentException("Unsupported data type: Class, please provide custom serializer for " + tpe)
//    } else {
//      BeanSerializer
//    }

    null

    //TODO Range, NumericRange
  }


  private def matches(tpe: Type, tpees: List[Type]) = tpees.contains(tpe)
}
