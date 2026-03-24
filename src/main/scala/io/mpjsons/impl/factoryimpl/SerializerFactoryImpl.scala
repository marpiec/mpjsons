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

object SerializerFactoryImpl {

  private lazy val primitiveSerializers: Map[Symbol, JsonTypeSerializer[_]] = Map(
    typeOf[Long].typeSymbol -> SimpleToStringSerializer,
    typeOf[Int].typeSymbol -> SimpleToStringSerializer,
    typeOf[Short].typeSymbol -> SimpleToStringSerializer,
    typeOf[Byte].typeSymbol -> SimpleToStringSerializer,
    typeOf[Boolean].typeSymbol -> SimpleToStringSerializer,
    typeOf[Double].typeSymbol -> SimpleToStringSerializer,
    typeOf[Float].typeSymbol -> SimpleToStringSerializer,
    typeOf[java.lang.Long].typeSymbol -> SimpleToStringSerializer,
    typeOf[java.lang.Integer].typeSymbol -> SimpleToStringSerializer,
    typeOf[java.lang.Short].typeSymbol -> SimpleToStringSerializer,
    typeOf[java.lang.Byte].typeSymbol -> SimpleToStringSerializer,
    typeOf[java.lang.Boolean].typeSymbol -> SimpleToStringSerializer,
    typeOf[java.lang.Double].typeSymbol -> SimpleToStringSerializer,
    typeOf[java.lang.Float].typeSymbol -> SimpleToStringSerializer
  )

  private lazy val stringSerializers: Set[Symbol] = Set(
    typeOf[String].typeSymbol,
    typeOf[mutable.StringBuilder].typeSymbol,
    typeOf[Char].typeSymbol,
    typeOf[java.lang.Character].typeSymbol
  )

  private lazy val localDateSymbol: Symbol = typeOf[java.time.LocalDate].typeSymbol
  private lazy val localTimeSymbol: Symbol = typeOf[java.time.LocalTime].typeSymbol
  private lazy val localDateTimeSymbol: Symbol = typeOf[java.time.LocalDateTime].typeSymbol
  private lazy val durationSymbol: Symbol = typeOf[java.time.Duration].typeSymbol

  private lazy val mutableSeqSymbol: Symbol = typeOf[mutable.Seq[_]].typeSymbol
  private lazy val mutableSetSymbol: Symbol = typeOf[mutable.Set[_]].typeSymbol
  private lazy val mutableMapSymbol: Symbol = typeOf[mutable.Map[_, _]].typeSymbol
  private lazy val mutableIterableSymbol: Symbol = typeOf[mutable.Iterable[_]].typeSymbol

  private lazy val collectionSeqSymbol: Symbol = typeOf[collection.Seq[_]].typeSymbol
  private lazy val bitSetSymbol: Symbol = typeOf[BitSet].typeSymbol
  private lazy val collectionSetSymbol: Symbol = typeOf[collection.Set[_]].typeSymbol
  private lazy val collectionMapSymbol: Symbol = typeOf[collection.Map[_, _]].typeSymbol
  private lazy val collectionIterableSymbol: Symbol = typeOf[Iterable[_]].typeSymbol

  private lazy val eitherSymbol: Symbol = typeOf[Either[_, _]].typeSymbol
  private lazy val optionSymbol: Symbol = typeOf[Option[_]].typeSymbol
  private lazy val tuple1Symbol: Symbol = typeOf[Tuple1[_]].typeSymbol
  private lazy val tuple2Symbol: Symbol = typeOf[Tuple2[_, _]].typeSymbol
  private lazy val tuple3Symbol: Symbol = typeOf[Tuple3[_, _, _]].typeSymbol
  private lazy val tuple4Symbol: Symbol = typeOf[Tuple4[_, _, _, _]].typeSymbol
  private lazy val tuple5Symbol: Symbol = typeOf[Tuple5[_, _, _, _, _]].typeSymbol
  private lazy val tuple6Symbol: Symbol = typeOf[Tuple6[_, _, _, _, _, _]].typeSymbol

  private lazy val nothingSymbol: Symbol = typeOf[Nothing].typeSymbol
  private lazy val anySymbol: Symbol = typeOf[Any].typeSymbol
  private lazy val anyRefSymbol: Symbol = typeOf[AnyRef].typeSymbol

}

class SerializerFactoryImpl(ignoreNullFields: Boolean) {

  import SerializerFactoryImpl._

  private var additionalSerializers: Map[String, SerializerFactory => JsonTypeSerializer[_]] = Map.empty
  private var additionalSerializersAnySubType: Map[String, SerializerFactory => JsonTypeSerializer[_]] = Map.empty
  private var additionalSuperclassSerializers: Map[Symbol, SerializerFactory => JsonTypeSerializer[_]] = Map.empty

  def registerSerializer[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]): Unit = {
    additionalSerializers += tpe.toString -> serializer
  }

  def registerSuperclassSerializer[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]): Unit = {
    additionalSuperclassSerializers += tpe.typeSymbol -> serializer
  }

  def registerSerializerAllSubTypes[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]): Unit = {
    additionalSerializersAnySubType += stripTypes(tpe.toString) -> serializer
  }

  private def stripTypes(typeName: String): String = {
    typeName.replaceAll("\\[.*\\]", "")
  }

  protected def getSerializerNoCache(tpe: Type, context: Context, allowSuperType: Boolean): JsonTypeSerializer[_] = {

    val typeSymbol = tpe.typeSymbol

    // Primitives
    primitiveSerializers.get(typeSymbol).foreach(return _)

    // Time types (allow override via additionalSerializers)
    val tpeString = tpe.toString
    if (typeSymbol == localDateSymbol && !additionalSerializers.contains(tpeString)) {
      return LocalDateSerializer
    } else if (typeSymbol == localTimeSymbol && !additionalSerializers.contains(tpeString)) {
      return LocalTimeSerializer
    } else if (typeSymbol == localDateTimeSymbol && !additionalSerializers.contains(tpeString)) {
      return LocalDateTimeSerializer
    } else if (typeSymbol == durationSymbol && !additionalSerializers.contains(tpeString)) {
      return DurationSerializer
    }

    // String, StringBuilder, Char
    if (stringSerializers.contains(typeSymbol)) {
      return StringSerializer
    }

    // Arrays
    tpe match {
      case typeRef: TypeRefApi if typeRef.sym == definitions.ArrayClass =>
        return new ArraySerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      case _ =>
    }

    // We don't want to support user's custom collections implicitly,
    // because there will be problem with deserialization, but we want to support standard subtypes
    if (typeSymbol.fullName.startsWith("scala.")) {

      val baseClasses = tpe.baseClasses

      //Every mutable collection
      if (baseClasses.contains(mutableSeqSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(mutableSetSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(mutableMapSymbol)) {
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(mutableIterableSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }

      //Every immutable collection
      if (baseClasses.contains(collectionSeqSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (typeSymbol == bitSetSymbol) {
        return new BitSetSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(collectionSetSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(collectionMapSymbol)) {
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(collectionIterableSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }

      if (baseClasses.contains(eitherSymbol)) {
        return new EitherSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(optionSymbol)) {
        return new OptionSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(tuple1Symbol)) {
        return new Tuple1Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(tuple2Symbol)) {
        return new Tuple2Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(tuple3Symbol)) {
        return new Tuple3Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(tuple4Symbol)) {
        return new Tuple4Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(tuple5Symbol)) {
        return new Tuple5Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (baseClasses.contains(tuple6Symbol)) {
        return new Tuple6Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
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



    val additionalSerializerOption = additionalSerializers.get(tpeString)

    if (additionalSerializerOption.isDefined) {
      additionalSerializerOption.get(this.asInstanceOf[SerializerFactory])
    } else if (nothingSymbol == typeSymbol) {
      throw new IllegalArgumentException("Serialization of 'Nothing' type is not supported, be sure to define types everywhere. Types: " + context.typesStackMessage)
    } else if (anySymbol == typeSymbol) {
      throw new IllegalArgumentException("Serialization of 'Any' or wildcard '_' type is not supported, be sure to define type more precisely. Types: " + context.typesStackMessage)
    } else if (anyRefSymbol == typeSymbol) {
      throw new IllegalArgumentException("Serialization of 'AnyRef' type is not supported, be sure to define type more precisely. Types: " + context.typesStackMessage)
    } else if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.field.getName == "MODULE$")) {
      SingletonObjectSerializer
    } else {

      val additionalOption = additionalSerializersAnySubType.get(stripTypes(tpeString))
      additionalOption match {
        case Some(serializer) => serializer(this.asInstanceOf[SerializerFactory])
        case None => new BeanSerializer(this.asInstanceOf[SerializerFactory], tpe, context, !ignoreNullFields)
      }

    }

    //TODO Range, NumericRange
  }

}
