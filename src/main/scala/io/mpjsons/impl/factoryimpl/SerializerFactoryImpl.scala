package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer._
import io.mpjsons.impl.util.reflection.ReflectionUtil

import scala.collection.immutable._
import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryImpl {

  private var additionalSerializers = Map[Type, SerializerFactory => JsonTypeSerializer[_]]()
  private var additionalSuperclassSerializers = Map[Symbol, SerializerFactory => JsonTypeSerializer[_]]()

  def registerSerializer[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]) {
    additionalSerializers += tpe -> serializer
  }

  def registerSuperclassSerializer[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]) {
    additionalSuperclassSerializers += tpe.typeSymbol -> serializer
  }

  protected def getSerializerNoCache(tpe: Type): JsonTypeSerializer[_] = {

    val typeSymbol = tpe.typeSymbol

    // Primitives, by toString (except Char)
    if (typeOf[Long].typeSymbol == typeSymbol) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Int].typeSymbol == typeSymbol) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Short].typeSymbol == typeSymbol) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Byte].typeSymbol == typeSymbol) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Boolean].typeSymbol == typeSymbol) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Double].typeSymbol == typeSymbol) {
      return new SimpleToStringSerializer()
    } else if (typeOf[Float].typeSymbol == typeSymbol) {
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
      return new ArraySerializer(this.asInstanceOf[SerializerFactory], tpe)
    }

    // We don't want to support user's custom collections implicitly,
    // because there will be problem with deserialization, but we want to support standard subtypes
    if (typeSymbol.fullName.startsWith("scala.")) {
      //Every immutable collection
      if (typeOf[Seq[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe)
      } else if (typeOf[Set[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe)
      } else if (tpe.baseClasses.contains(typeOf[Map[_, _]].typeSymbol)) {
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe)
      } else if (typeOf[BitSet].typeSymbol == typeSymbol) {
          return new BitSetSerializer(this.asInstanceOf[SerializerFactory], tpe)
      } else if (tpe.baseClasses.contains(typeOf[Iterable[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe)
      }

      //Every mutable collection
      if (typeOf[mutable.Seq[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe)
      } else if (typeOf[mutable.Set[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe)
      } else if (typeOf[mutable.Map[_, _]].typeSymbol == typeSymbol) {
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe)
      } else if (typeOf[mutable.Iterable[_]].typeSymbol == typeSymbol) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe)
      }

      if(tpe.baseClasses.contains(typeOf[Either[_, _]].typeSymbol)) {
        return new EitherSerializer(this.asInstanceOf[SerializerFactory], tpe)
      }

      // Every Tuple, Option
      if (tpe.baseClasses.contains(typeOf[Product].typeSymbol)) {
        return new ProductSerializer(this.asInstanceOf[SerializerFactory], tpe)
      }

    }


    additionalSuperclassSerializers.get(typeSymbol) match {
      case Some(serializer) => return serializer(this.asInstanceOf[SerializerFactory])
      case None =>
        for ((tpeType, serializer) <- additionalSuperclassSerializers) {
          if (tpe.baseClasses.contains(tpeType)) {
            return serializer(this.asInstanceOf[SerializerFactory])
          }
        }
    }


    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.field.getName == "MODULE$")) {
      return new SingletonObjectSerializer()
    }

    val additionalSerializerOption = additionalSerializers.get(tpe)

    if (additionalSerializerOption.isDefined) {
      additionalSerializerOption.get(this.asInstanceOf[SerializerFactory])
    } else if(typeOf[Nothing].typeSymbol == typeSymbol) {
      throw new IllegalArgumentException("Serialization of 'Nothing' type is not supported, be sure to define types everywhere.")
    } else {
      new BeanSerializer(this.asInstanceOf[SerializerFactory], tpe)
    }

    //TODO Range, NumericRange
  }

}
