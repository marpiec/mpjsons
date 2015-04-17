package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.serializer._
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil

import scala.collection.immutable._
import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryImpl {

  private var additionalSerializers = Map[Type, JsonTypeSerializer[_]]()
  private var additionalSuperclassSerializers = Map[Symbol, JsonTypeSerializer[_]]()

  def registerSerializer(tpe: Type, serializer: JsonTypeSerializer[_]) {
    additionalSerializers += tpe -> serializer
  }

  def registerSuperclassSerializer(tpe: Type, serializer: JsonTypeSerializer[_]) {
    additionalSuperclassSerializers += tpe.typeSymbol -> serializer
  }

  def getSerializer(tpe: Type): JsonTypeSerializer[_] = {


    // Primitives, by toString (except Char)
    if (typeOf[Long] == tpe || typeOf[java.lang.Long] == tpe) {
      return SimpleToStringSerializer
    } else if (typeOf[Int] == tpe || typeOf[java.lang.Integer] == tpe) {
      return SimpleToStringSerializer
    } else if (typeOf[Short] == tpe || typeOf[java.lang.Short] == tpe) {
      return SimpleToStringSerializer
    } else if (typeOf[Byte] == tpe || typeOf[java.lang.Byte] == tpe) {
      return SimpleToStringSerializer
    } else if (typeOf[Boolean] == tpe || typeOf[java.lang.Boolean] == tpe) {
      return SimpleToStringSerializer
    } else if (typeOf[Double] == tpe || typeOf[java.lang.Double] == tpe) {
      return SimpleToStringSerializer
    } else if (typeOf[Float] == tpe || typeOf[java.lang.Float] == tpe) {
      return SimpleToStringSerializer
    }

    // String, StringBuilder, Char
    if (typeOf[String].typeSymbol == tpe.typeSymbol ||
      typeOf[mutable.StringBuilder].typeSymbol == tpe.typeSymbol ||
      typeOf[Char].typeSymbol == tpe.typeSymbol || typeOf[java.lang.Character].typeSymbol == tpe.typeSymbol) {
      return StringSerializer
    }

    // Arrays
    if (tpe.isInstanceOf[TypeRefApi] && tpe.asInstanceOf[TypeRefApi].sym == definitions.ArrayClass) {
      return ArraySerializer
    }

    // We don't want to support user's custom collections implicitly,
    // because there will be problem with deserialization, but we want to support standard subtypes
    if (tpe.typeSymbol.fullName.startsWith("scala.")) {
      //Every immutable collection
      if (typeOf[Seq[_]].typeSymbol == tpe.typeSymbol) {
        return IterableSerializer
      } else if (typeOf[Set[_]].typeSymbol == tpe.typeSymbol) {
        return IterableSerializer
      } else if (tpe.baseClasses.contains(typeOf[Map[_, _]].typeSymbol)) {
        return MapSerializer
      } else if (tpe.baseClasses.contains(typeOf[Iterable[_]].typeSymbol)) {
        return IterableSerializer
      }

      //Every mutable collection
      if (typeOf[mutable.Seq[_]].typeSymbol == tpe.typeSymbol) {
        return IterableSerializer
      } else if (typeOf[mutable.Set[_]].typeSymbol == tpe.typeSymbol) {
        return IterableSerializer
      } else if (typeOf[mutable.Map[_, _]].typeSymbol == tpe.typeSymbol) {
        return MapSerializer
      } else if (typeOf[mutable.Iterable[_]].typeSymbol == tpe.typeSymbol) {
        return IterableSerializer
      }

      if(tpe.baseClasses.contains(typeOf[Either[_, _]].typeSymbol)) {
        return EitherSerializer
      }

      // Every Tuple, Option
      if (tpe.baseClasses.contains(typeOf[Product].typeSymbol)) {
        return ProductSerializer
      }

    }


    additionalSuperclassSerializers.get(tpe.typeSymbol) match {
      case Some(serializer) => return serializer
      case None =>
        for ((tpeType, serializer) <- additionalSuperclassSerializers) {
          if (tpe.baseClasses.contains(tpeType)) {
            return serializer
          }
        }
    }


    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.getName == "MODULE$")) {
      return SingletonObjectSerializer
    }

    val additionalSerializerOption = additionalSerializers.get(tpe)

    if (additionalSerializerOption.isDefined) {
      additionalSerializerOption.get
    } else {
      BeanSerializer
    }

    //TODO Range, NumericRange
  }

}
