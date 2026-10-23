package io.mpjsons.legacy.impl.factoryimpl

import io.mpjsons.legacy.{JsonTypeSerializer, MPJsonsConfig}
import io.mpjsons.legacy.impl.SerializerFactory
import io.mpjsons.legacy.impl.serializer._
import io.mpjsons.legacy.impl.serializer.time._
import io.mpjsons.legacy.impl.util.{Context, MapKeys, TypesUtil}

import scala.collection.immutable._
import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactoryImpl(val config: MPJsonsConfig) {

  private var additionalSerializers: Map[String, SerializerFactory => JsonTypeSerializer[_]] = Map.empty
  private var additionalSerializersAnySubType: Map[String, SerializerFactory => JsonTypeSerializer[_]] = Map.empty
  private var additionalSuperclassSerializers: Map[Symbol, SerializerFactory => JsonTypeSerializer[_]] = Map.empty

  /** Called after every registration; the caching layer overrides it to drop (de)serializers built earlier. */
  protected def registrationsChanged(): Unit = ()

  def registerSerializer[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]): Unit = {
    additionalSerializers += tpe.toString -> serializer
    registrationsChanged()
  }

  def registerSuperclassSerializer[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]): Unit = {
    additionalSuperclassSerializers += tpe.typeSymbol -> serializer
    registrationsChanged()
  }

  def registerSerializerAllSubTypes[T](tpe: Type, serializer: SerializerFactory => JsonTypeSerializer[T]): Unit = {
    additionalSerializersAnySubType += stripTypes(tpe.toString) -> serializer
    registrationsChanged()
  }

  private def stripTypes(typeName: String): String = {
    typeName.replaceAll("\\[.*\\]", "")
  }

  protected def getSerializerNoCache(tpe: Type, context: Context, allowSuperType: Boolean): JsonTypeSerializer[_] = {

    val typeSymbol = tpe.typeSymbol

    // Primitives, by toString (except Char)
    if (typeOf[Long].typeSymbol == typeSymbol) {
      return LongSerializer
    } else if (typeOf[Int].typeSymbol == typeSymbol) {
      return IntSerializer
    } else if (typeOf[Short].typeSymbol == typeSymbol) {
      return ShortSerializer
    } else if (typeOf[Byte].typeSymbol == typeSymbol) {
      return ByteSerializer
    } else if (typeOf[Boolean].typeSymbol == typeSymbol) {
      return BooleanSerializer
    } else if (typeOf[Double].typeSymbol == typeSymbol) {
      return DoubleSerializer
    } else if (typeOf[Float].typeSymbol == typeSymbol) {
      return FloatSerializer
    } else if (typeOf[java.lang.Long].typeSymbol == typeSymbol) {
      return LongSerializer
    } else if (typeOf[java.lang.Integer].typeSymbol == typeSymbol) {
      return IntSerializer
    } else if (typeOf[java.lang.Short].typeSymbol == typeSymbol) {
      return ShortSerializer
    } else if (typeOf[java.lang.Byte].typeSymbol == typeSymbol) {
      return ByteSerializer
    } else if (typeOf[java.lang.Boolean].typeSymbol == typeSymbol) {
      return BooleanSerializer
    } else if (typeOf[java.lang.Double].typeSymbol == typeSymbol) {
      return DoubleSerializer
    } else if (typeOf[java.lang.Float].typeSymbol == typeSymbol) {
      return FloatSerializer
    } else if (typeOf[java.time.LocalDate].typeSymbol == typeSymbol && !additionalSerializers.contains(tpe.toString)) {
      return (if (config.isoTime) IsoLocalDateSerializer else LocalDateSerializer)
    } else if (typeOf[java.time.LocalTime].typeSymbol == typeSymbol && !additionalSerializers.contains(tpe.toString)) {
      return (if (config.isoTime) IsoLocalTimeSerializer else LocalTimeSerializer)
    } else if (typeOf[java.time.LocalDateTime].typeSymbol == typeSymbol && !additionalSerializers.contains(tpe.toString)) {
      return (if (config.isoTime) IsoLocalDateTimeSerializer else LocalDateTimeSerializer)
    } else if (typeOf[java.time.Duration].typeSymbol == typeSymbol && !additionalSerializers.contains(tpe.toString)) {
      return (if (config.isoTime) IsoDurationSerializer else DurationSerializer)
    }



    // Arbitrary precision numbers, UUID and the zoned java.time types (unless a converter is registered)
    if (!additionalSerializers.contains(tpe.toString)) {
      if (typeOf[BigDecimal].typeSymbol == typeSymbol || typeOf[java.math.BigDecimal].typeSymbol == typeSymbol) {
        return BigDecimalSerializer
      } else if (typeOf[BigInt].typeSymbol == typeSymbol || typeOf[java.math.BigInteger].typeSymbol == typeSymbol) {
        return BigIntSerializer
      } else if (typeOf[java.util.UUID].typeSymbol == typeSymbol || typeOf[java.time.Instant].typeSymbol == typeSymbol ||
        typeOf[java.time.OffsetDateTime].typeSymbol == typeSymbol || typeOf[java.time.ZonedDateTime].typeSymbol == typeSymbol) {
        return new ToStringAsJsonStringSerializer(if (config.escapeSlash) StringSerializer else StringSerializer.withoutSlashEscape)
      }
    }

    // String, StringBuilder, Char

    if (typeOf[String].typeSymbol == typeSymbol ||
      typeOf[mutable.StringBuilder].typeSymbol == typeSymbol ||
      typeOf[Char].typeSymbol == typeSymbol || typeOf[java.lang.Character].typeSymbol == typeSymbol) {
      return (if (config.escapeSlash) StringSerializer else StringSerializer.withoutSlashEscape)
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
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe, context, config.mapsAsObjects && MapKeys.supportsObjectForm(TypesUtil.getDoubleSubElementsType(tpe)._1))
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
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe, context, config.mapsAsObjects && MapKeys.supportsObjectForm(TypesUtil.getDoubleSubElementsType(tpe)._1))
      } else if (tpe.baseClasses.contains(typeOf[Iterable[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }

      // Other
      if (tpe.baseClasses.contains(typeOf[scala.collection.Seq[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[scala.collection.Set[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[scala.collection.Map[_, _]].typeSymbol)) {
        return new MapSerializer(this.asInstanceOf[SerializerFactory], tpe, context, config.mapsAsObjects && MapKeys.supportsObjectForm(TypesUtil.getDoubleSubElementsType(tpe)._1))
      } else if (tpe.baseClasses.contains(typeOf[scala.collection.Iterable[_]].typeSymbol)) {
        return new IterableSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }


      if (tpe.baseClasses.contains(typeOf[Either[_, _]].typeSymbol)) {
        return new EitherSerializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[Option[_]].typeSymbol)) {
        return new OptionSerializer(this.asInstanceOf[SerializerFactory], tpe, context, config.compactOptions)
      } else if (tpe.baseClasses.contains(typeOf[Tuple1[_]].typeSymbol)) {
        return new Tuple1Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[Tuple2[_, _]].typeSymbol)) {
        return new Tuple2Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[Tuple3[_, _, _]].typeSymbol)) {
        return new Tuple3Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[Tuple4[_, _, _, _]].typeSymbol)) {
        return new Tuple4Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[Tuple5[_, _, _, _, _]].typeSymbol)) {
        return new Tuple5Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      } else if (tpe.baseClasses.contains(typeOf[Tuple6[_, _, _, _, _, _]].typeSymbol)) {
        return new Tuple6Serializer(this.asInstanceOf[SerializerFactory], tpe, context)
      }


    }


    if(allowSuperType) {
      // The most specific registered supertype wins: baseClasses is the linearization, most derived first.
      val registered = tpe.baseClasses.iterator.flatMap(additionalSuperclassSerializers.get).nextOption()
      if (registered.isDefined) {
        return registered.get(this.asInstanceOf[SerializerFactory])
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
    } else if (typeSymbol.isModuleClass) {
      SingletonObjectSerializer
    } else {


      val additionalOption = additionalSerializersAnySubType.get(stripTypes(tpe.toString))
      additionalOption match {
        case Some(serializer) => serializer(this.asInstanceOf[SerializerFactory])
        case None if typeSymbol.fullName.startsWith("java.") =>
          throw new IllegalArgumentException("No built-in serializer for java type " + tpe + ", use registerConverter. Types: " + context.typesStackMessage)
        case None if typeSymbol.isAbstract && !typeSymbol.isModuleClass =>
          // Only the abstract type's own fields would be written and the concrete class would be lost.
          throw new IllegalArgumentException("Cannot serialize abstract type " + tpe + " (trait or abstract class) without a registered converter, " +
            "use markTypedClass to write the concrete type, or registerConverter, registerSuperclassConverter or registerSerializerForAnySubtype. " +
            "Types: " + context.typesStackMessage)
        case None => new BeanSerializer(this.asInstanceOf[SerializerFactory], tpe, context, config)
      }

    }

    //TODO Range, NumericRange
  }

}
