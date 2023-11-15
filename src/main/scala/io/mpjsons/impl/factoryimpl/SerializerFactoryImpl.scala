package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer._
import io.mpjsons.impl.serializer.time.{DurationSerializer, LocalDateSerializer, LocalDateTimeSerializer, LocalTimeSerializer}
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.util.reflection.ReflectionUtil

import scala.collection.immutable._
import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryImpl(ignoreNullFields: Boolean) {

  private var additionalSerializers: Map[String, SerializerFactory => JsonTypeSerializer[_]] = Map.empty
  private var additionalSuperclassSerializers: Map[Symbol, SerializerFactory => JsonTypeSerializer[_]] = Map.empty

  def registerSerializer[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]): Unit = {
    additionalSerializers += tpe.toString -> serializer
  }

  def registerSuperclassSerializer[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]): Unit = {
    additionalSuperclassSerializers += tpe.typeSymbol -> serializer
  }

  protected def getSerializerNoCache(tpe: Type, context: Context, allowSuperType: Boolean): JsonTypeSerializer[_] = {

    val typeSymbol = tpe.typeSymbol

    // Primitives, by toString (except Char)
    if (typeOf[Long].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[Int].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[Short].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[Byte].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[Boolean].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[Double].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[Float].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[java.lang.Long].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[java.lang.Integer].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[java.lang.Short].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[java.lang.Byte].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[java.lang.Boolean].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[java.lang.Double].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[java.lang.Float].typeSymbol == typeSymbol) {
      return SimpleToStringSerializer
    } else if (typeOf[java.time.LocalDate].typeSymbol == typeSymbol && !additionalSerializers.contains(tpe.toString)) {
      return LocalDateSerializer
    } else if (typeOf[java.time.LocalTime].typeSymbol == typeSymbol && !additionalSerializers.contains(tpe.toString)) {
      return LocalTimeSerializer
    } else if (typeOf[java.time.LocalDateTime].typeSymbol == typeSymbol && !additionalSerializers.contains(tpe.toString)) {
      return LocalDateTimeSerializer
    } else if (typeOf[java.time.Duration].typeSymbol == typeSymbol && !additionalSerializers.contains(tpe.toString)) {
      return DurationSerializer
    }



    // String, StringBuilder, Char

    if (typeOf[String].typeSymbol == typeSymbol ||
      typeOf[mutable.StringBuilder].typeSymbol == typeSymbol ||
      typeOf[Char].typeSymbol == typeSymbol || typeOf[java.lang.Character].typeSymbol == typeSymbol) {
      return StringSerializer
    }

    // Arrays
    if (tpe.isInstanceOf[TypeRefApi] && tpe.asInstanceOf[TypeRefApi].sym == definitions.ArrayClass) {
      return new ArraySerializer(this.asInstanceOf[SerializerFactory], tpe, context)
    }

    // We don't want to support user's custom collections implicitly,
    // because there will be problem with deserialization, but we want to support standard subtypes
    if (typeSymbol.fullName.startsWith("scala.")) {


      //Every mutable collection
      if (tpe.baseClasses.contains(typeOf[mutable.Seq[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[mutable.Set[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[mutable.Map[_, _]].typeSymbol)) {
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[mutable.Iterable[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }

      //Every immutable collection
      if (tpe.baseClasses.contains(typeOf[collection.Seq[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (typeOf[BitSet].typeSymbol == typeSymbol) {
        return new BitSetSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[collection.Set[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[collection.Map[_, _]].typeSymbol)) {
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[Iterable[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }

      // Other
      if (tpe.baseClasses.contains(typeOf[scala.collection.Seq[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[scala.collection.Set[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[scala.collection.Map[_, _]].typeSymbol)) {
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[scala.collection.Iterable[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }


      if (tpe.baseClasses.contains(typeOf[Either[_, _]].typeSymbol)) {
        return new EitherSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }

      if (tpe.baseClasses.contains(typeOf[Option[_]].typeSymbol)) {
        return new OptionSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }

      // Every Tuple, Option
      if (tpe.baseClasses.contains(typeOf[Product].typeSymbol)) {
        return new ProductSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }

    }


    if(allowSuperType) {
      additionalSuperclassSerializers.get(typeSymbol) match {
        case Some(serializer) => return serializer(this.asInstanceOf[SerializerFactory])
        case None =>
          for ((tpeType, serializer) <- additionalSuperclassSerializers) {
            if (tpe.baseClasses.contains(tpeType)) {
              return serializer(this.asInstanceOf[SerializerFactory])
            }
          }
      }
    }




    val additionalSerializerOption = additionalSerializers.get(tpe.toString)

    if (additionalSerializerOption.isDefined) {
      additionalSerializerOption.get(this.asInstanceOf[SerializerFactory])
    } else if (typeOf[Nothing].typeSymbol == typeSymbol) {
      throw new IllegalArgumentException("Serialization of 'Nothing' type is not supported, be sure to define types everywhere. Types: " + context.typesStackMessage)
    } else if (typeOf[Any].typeSymbol == typeSymbol) {
      throw new IllegalArgumentException("Serialization of 'Any' or wildcard '_' type is not supported, be sure to define type more precisely. Types: " + context.typesStackMessage)
    } else if (typeOf[AnyRef].typeSymbol == typeSymbol) {
      throw new IllegalArgumentException("Serialization of 'AnyRef' type is not supported, be sure to define type more precisely. Types: " + context.typesStackMessage)
    } else if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.field.getName == "MODULE$")) {
      SingletonObjectSerializer
    } else {
      new BeanSerializer(this.asInstanceOf[SerializerFactory], tpe, context, !ignoreNullFields)
    }

    //TODO Range, NumericRange
  }

}
