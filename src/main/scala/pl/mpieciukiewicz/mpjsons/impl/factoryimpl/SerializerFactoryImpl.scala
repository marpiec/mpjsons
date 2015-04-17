package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.{MPJsons, JsonTypeSerializer}
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

    val typeSymbol = tpe.typeSymbol
    val serializerFactory = MPJsons.serializerFactory

    // Primitives, by toString (except Char)
    if (typeOf[Long] == tpe || typeOf[java.lang.Long] == tpe) {
      return SimpleToStringSerializer()
    } else if (typeOf[Int] == tpe || typeOf[java.lang.Integer] == tpe) {
      return SimpleToStringSerializer()
    } else if (typeOf[Short] == tpe || typeOf[java.lang.Short] == tpe) {
      return SimpleToStringSerializer()
    } else if (typeOf[Byte] == tpe || typeOf[java.lang.Byte] == tpe) {
      return SimpleToStringSerializer()
    } else if (typeOf[Boolean] == tpe || typeOf[java.lang.Boolean] == tpe) {
      return SimpleToStringSerializer()
    } else if (typeOf[Double] == tpe || typeOf[java.lang.Double] == tpe) {
      return SimpleToStringSerializer()
    } else if (typeOf[Float] == tpe || typeOf[java.lang.Float] == tpe) {
      return SimpleToStringSerializer()
    }


    // String, StringBuilder, Char

    if (typeOf[String].typeSymbol == typeSymbol ||
      typeOf[mutable.StringBuilder].typeSymbol == typeSymbol ||
      typeOf[Char].typeSymbol == typeSymbol || typeOf[java.lang.Character].typeSymbol == typeSymbol) {
      return StringSerializer()
    }

    // Arrays
    if (tpe.isInstanceOf[TypeRefApi] && tpe.asInstanceOf[TypeRefApi].sym == definitions.ArrayClass) {
      return ArraySerializer(serializerFactory)
    }

    // We don't want to support user's custom collections implicitly,
    // because there will be problem with deserialization, but we want to support standard subtypes
    if (typeSymbol.fullName.startsWith("scala.")) {
      //Every immutable collection
      if (typeOf[Seq[_]].typeSymbol == typeSymbol) {
        return IterableSerializer(serializerFactory)
      } else if (typeOf[Set[_]].typeSymbol == typeSymbol) {
        return IterableSerializer(serializerFactory)
      } else if (tpe.baseClasses.contains(typeOf[Map[_, _]].typeSymbol)) {
        return MapSerializer(serializerFactory)
      } else if (tpe.baseClasses.contains(typeOf[Iterable[_]].typeSymbol)) {
        return IterableSerializer(serializerFactory)
      }

      //Every mutable collection
      if (typeOf[mutable.Seq[_]].typeSymbol == typeSymbol) {
        return IterableSerializer(serializerFactory)
      } else if (typeOf[mutable.Set[_]].typeSymbol == typeSymbol) {
        return IterableSerializer(serializerFactory)
      } else if (typeOf[mutable.Map[_, _]].typeSymbol == typeSymbol) {
        return MapSerializer(serializerFactory)
      } else if (typeOf[mutable.Iterable[_]].typeSymbol == typeSymbol) {
        return IterableSerializer(serializerFactory)
      }

      if(tpe.baseClasses.contains(typeOf[Either[_, _]].typeSymbol)) {
        return EitherSerializer(serializerFactory)
      }

      // Every Tuple, Option
      if (tpe.baseClasses.contains(typeOf[Product].typeSymbol)) {
        return ProductSerializer(serializerFactory)
      }

    }


    additionalSuperclassSerializers.get(typeSymbol) match {
      case Some(serializer) => return serializer
      case None =>
        for ((tpeType, serializer) <- additionalSuperclassSerializers) {
          if (tpe.baseClasses.contains(tpeType)) {
            return serializer
          }
        }
    }


    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.getName == "MODULE$")) {
      return SingletonObjectSerializer()
    }

    val additionalSerializerOption = additionalSerializers.get(tpe)

    if (additionalSerializerOption.isDefined) {
      additionalSerializerOption.get
    } else {
      BeanSerializer(serializerFactory)
    }

    //TODO Range, NumericRange
  }

}
