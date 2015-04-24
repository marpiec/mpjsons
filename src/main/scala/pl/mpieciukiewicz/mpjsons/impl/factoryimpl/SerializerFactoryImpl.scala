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
      return new SimpleToStringSerializer()
    } else if (typeOf[Int] == tpe || typeOf[java.lang.Integer] == tpe) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Short] == tpe || typeOf[java.lang.Short] == tpe) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Byte] == tpe || typeOf[java.lang.Byte] == tpe) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Boolean] == tpe || typeOf[java.lang.Boolean] == tpe) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Double] == tpe || typeOf[java.lang.Double] == tpe) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Float] == tpe || typeOf[java.lang.Float] == tpe) {
      return new SimpleToStringSerializer()
    }


    // String, StringBuilder, Char

    if (typeOf[String].typeSymbol == typeSymbol ||
      typeOf[mutable.StringBuilder].typeSymbol == typeSymbol ||
      typeOf[Char].typeSymbol == typeSymbol || typeOf[java.lang.Character].typeSymbol == typeSymbol) {
      return new StringSerializer()
    }

    // Arrays
    if (tpe.isInstanceOf[TypeRefApi] && tpe.asInstanceOf[TypeRefApi].sym == definitions.ArrayClass) {
      return new ArraySerializer(serializerFactory, tpe)
    }

    // We don't want to support user's custom collections implicitly,
    // because there will be problem with deserialization, but we want to support standard subtypes
    if (typeSymbol.fullName.startsWith("scala.")) {
      //Every immutable collection
      if (typeOf[Seq[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(serializerFactory, tpe)
      } else if (typeOf[Set[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(serializerFactory, tpe)
      } else if (tpe.baseClasses.contains(typeOf[Map[_, _]].typeSymbol)) {
        return new MapSerializer(serializerFactory, tpe)
      } else if (typeOf[BitSet].typeSymbol == typeSymbol) {
          return new BitSetSerializer(serializerFactory, tpe)
      } else if (tpe.baseClasses.contains(typeOf[Iterable[_]].typeSymbol)) {
        return new IterableSerializer(serializerFactory, tpe)
      }

      //Every mutable collection
      if (typeOf[mutable.Seq[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(serializerFactory, tpe)
      } else if (typeOf[mutable.Set[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(serializerFactory, tpe)
      } else if (typeOf[mutable.Map[_, _]].typeSymbol == typeSymbol) {
        return new MapSerializer(serializerFactory, tpe)
      } else if (typeOf[mutable.Iterable[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(serializerFactory, tpe)
      }

      if(tpe.baseClasses.contains(typeOf[Either[_, _]].typeSymbol)) {
        return new EitherSerializer(serializerFactory, tpe)
      }

      // Every Tuple, Option
      if (tpe.baseClasses.contains(typeOf[Product].typeSymbol)) {
        return new ProductSerializer(serializerFactory, tpe)
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


    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.field.getName == "MODULE$")) {
      return new SingletonObjectSerializer()
    }

    val additionalSerializerOption = additionalSerializers.get(tpe)

    if (additionalSerializerOption.isDefined) {
      additionalSerializerOption.get
    } else {
      new BeanSerializer(serializerFactory, tpe)
    }

    //TODO Range, NumericRange
  }

}
