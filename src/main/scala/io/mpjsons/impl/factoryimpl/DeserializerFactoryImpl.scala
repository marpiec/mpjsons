package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.DeserializerFactory
import io.mpjsons.impl.deserializer.immutables._
import io.mpjsons.impl.deserializer.mutables.ArrayDeserializer
import io.mpjsons.impl.deserializer.time.{DurationDeserializer, LocalDateDeserializer, LocalDateTimeDeserializer, LocalTimeDeserializer}
import io.mpjsons.impl.deserializer.utiltypes.{EitherDeserializer, Tuple1Deserializer, Tuple2Deserializer, Tuple3Deserializer, Tuple4Deserializer, Tuple5Deserializer, Tuple6Deserializer}
import io.mpjsons.impl.deserializer.values._
import io.mpjsons.impl.deserializer.{BeanDeserializer, PostTransformDeserializer, SingletonObjectDeserializer}
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.util.reflection.ReflectionUtil

import scala.collection._
import scala.collection.immutable.Map
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object DeserializerFactoryImpl {

  private lazy val primitiveDeserializers: Map[Symbol, JsonTypeDeserializer[_]] = Map(
    typeOf[Long].typeSymbol -> LongDeserializer,
    typeOf[Int].typeSymbol -> IntDeserializer,
    typeOf[Boolean].typeSymbol -> BooleanDeserializer,
    typeOf[String].typeSymbol -> StringDeserializer,
    typeOf[Double].typeSymbol -> DoubleDeserializer,
    typeOf[Float].typeSymbol -> FloatDeserializer,
    typeOf[Short].typeSymbol -> ShortDeserializer,
    typeOf[Byte].typeSymbol -> ByteDeserializer,
    typeOf[Char].typeSymbol -> CharDeserializer,
    typeOf[java.lang.Long].typeSymbol -> LongDeserializer,
    typeOf[java.lang.Integer].typeSymbol -> IntDeserializer,
    typeOf[java.lang.Boolean].typeSymbol -> BooleanDeserializer,
    typeOf[java.lang.Double].typeSymbol -> DoubleDeserializer,
    typeOf[java.lang.Float].typeSymbol -> FloatDeserializer,
    typeOf[java.lang.Short].typeSymbol -> ShortDeserializer,
    typeOf[java.lang.Byte].typeSymbol -> ByteDeserializer,
    typeOf[java.lang.Character].typeSymbol -> CharDeserializer
  )

  private lazy val localTimeSymbol: Symbol = typeOf[java.time.LocalTime].typeSymbol
  private lazy val localDateSymbol: Symbol = typeOf[java.time.LocalDate].typeSymbol
  private lazy val localDateTimeSymbol: Symbol = typeOf[java.time.LocalDateTime].typeSymbol
  private lazy val durationSymbol: Symbol = typeOf[java.time.Duration].typeSymbol

  private lazy val optionSymbol: Symbol = typeOf[Option[_]].typeSymbol
  private lazy val mapSymbol: Symbol = typeOf[Map[_, _]].typeSymbol
  private lazy val immutableMapSymbol: Symbol = typeOf[immutable.Map[_, _]].typeSymbol
  private lazy val eitherSymbol: Symbol = typeOf[Either[_, _]].typeSymbol

  private lazy val tuple1Symbol: Symbol = typeOf[Tuple1[_]].typeSymbol
  private lazy val tuple2Symbol: Symbol = typeOf[Tuple2[_, _]].typeSymbol
  private lazy val tuple3Symbol: Symbol = typeOf[Tuple3[_, _, _]].typeSymbol
  private lazy val tuple4Symbol: Symbol = typeOf[Tuple4[_, _, _, _]].typeSymbol
  private lazy val tuple5Symbol: Symbol = typeOf[Tuple5[_, _, _, _, _]].typeSymbol
  private lazy val tuple6Symbol: Symbol = typeOf[Tuple6[_, _, _, _, _, _]].typeSymbol

  private lazy val listSymbol: Symbol = typeOf[List[_]].typeSymbol
  private lazy val immutableListSymbol: Symbol = typeOf[immutable.List[_]].typeSymbol
  private lazy val vectorSymbol: Symbol = typeOf[Vector[_]].typeSymbol
  private lazy val immutableVectorSymbol: Symbol = typeOf[immutable.Vector[_]].typeSymbol
  private lazy val iterableSymbol: Symbol = typeOf[Iterable[_]].typeSymbol
  private lazy val immutableIterableSymbol: Symbol = typeOf[immutable.Iterable[_]].typeSymbol
  private lazy val seqSymbol: Symbol = typeOf[Seq[_]].typeSymbol
  private lazy val immutableSeqSymbol: Symbol = typeOf[immutable.Seq[_]].typeSymbol
  @annotation.nowarn("cat=deprecation")
  private lazy val streamSymbol: Symbol = typeOf[Stream[_]].typeSymbol
  @annotation.nowarn("cat=deprecation")
  private lazy val immutableStreamSymbol: Symbol = typeOf[immutable.Stream[_]].typeSymbol
  private lazy val queueSymbol: Symbol = typeOf[immutable.Queue[_]].typeSymbol

  private lazy val setSymbol: Symbol = typeOf[Set[_]].typeSymbol
  private lazy val immutableSetSymbol: Symbol = typeOf[immutable.Set[_]].typeSymbol
  private lazy val hashSetSymbol: Symbol = typeOf[immutable.HashSet[_]].typeSymbol
  private lazy val listSetSymbol: Symbol = typeOf[immutable.ListSet[_]].typeSymbol
  private lazy val sortedSetSymbol: Symbol = typeOf[immutable.SortedSet[_]].typeSymbol
  private lazy val treeSetSymbol: Symbol = typeOf[immutable.TreeSet[_]].typeSymbol
  private lazy val bitSetSymbol: Symbol = typeOf[BitSet].typeSymbol
  private lazy val immutableBitSetSymbol: Symbol = typeOf[immutable.BitSet].typeSymbol

  private lazy val hashMapSymbol: Symbol = typeOf[immutable.HashMap[_, _]].typeSymbol
  private lazy val sortedMapSymbol: Symbol = typeOf[immutable.SortedMap[_, _]].typeSymbol
  private lazy val treeMapSymbol: Symbol = typeOf[immutable.TreeMap[_, _]].typeSymbol
  private lazy val listMapSymbol: Symbol = typeOf[immutable.ListMap[_, _]].typeSymbol

  private lazy val listBufferSymbol: Symbol = typeOf[ListBuffer[_]].typeSymbol
  private lazy val nothingSymbol: Symbol = typeOf[Nothing].typeSymbol
  private lazy val anySymbol: Symbol = typeOf[Any].typeSymbol
  private lazy val anyRefSymbol: Symbol = typeOf[AnyRef].typeSymbol

}

class DeserializerFactoryImpl(ignoreNonExistingFields: Boolean) {

  import DeserializerFactoryImpl._

  private var additionalDeserializers: Map[String, DeserializerFactory => JsonTypeDeserializer[_]] = Map.empty
  private var additionalSuperclassDeserializers: Map[Symbol, DeserializerFactory => JsonTypeDeserializer[_]] = Map.empty
  private var postTransform: Map[Type, _ => _] = Map.empty

  def registerPostTransform[T](tpe: Type, transform: T => T): Unit = {
    postTransform += tpe -> transform
  }

  def registerDeserializer[T](tpe: Type, deserializer: DeserializerFactory => JsonTypeDeserializer[T]): Unit = {
    additionalDeserializers += tpe.toString -> deserializer
  }

  def registerSuperclassDeserializer[T](tpe: Type, deserializer: DeserializerFactory => JsonTypeDeserializer[T]): Unit = {
    additionalSuperclassDeserializers += tpe.typeSymbol -> deserializer
  }

  protected def getDeserializerNoCache(tpe: Type, context: Context, allowSuperType: Boolean): JsonTypeDeserializer[_ <: Any] = {
    val deserializer: JsonTypeDeserializer[_ <: Any] = getPureDeserializerNoCache(tpe, context, allowSuperType)
    postTransform.get(tpe) match {
      case Some(transform) => new PostTransformDeserializer[Any](deserializer, transform.asInstanceOf[(Any) => Any])
      case None => deserializer
    }
  }

  private def getPureDeserializerNoCache(tpe: Type, context: Context, allowSuperType: Boolean): JsonTypeDeserializer[_ <: Any] = {

    val typeSymbol = tpe.typeSymbol

    // Primitives and string types
    primitiveDeserializers.get(typeSymbol).foreach(return _)

    // Time types (allow override via additionalDeserializers)
    val tpeString = tpe.toString
    if (typeSymbol == localTimeSymbol && !additionalDeserializers.contains(tpeString)) {
      return LocalTimeDeserializer
    } else if (typeSymbol == localDateSymbol && !additionalDeserializers.contains(tpeString)) {
      return LocalDateDeserializer
    } else if (typeSymbol == localDateTimeSymbol && !additionalDeserializers.contains(tpeString)) {
      return LocalDateTimeDeserializer
    } else if (typeSymbol == durationSymbol && !additionalDeserializers.contains(tpeString)) {
      return DurationDeserializer
    }

    // Arrays
    tpe match {
      case typeRef: TypeRefApi if typeRef.sym == definitions.ArrayClass =>
        return new ArrayDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
      case _ =>
    }

    if (typeSymbol == optionSymbol) {
      return new OptionDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == mapSymbol || typeSymbol == immutableMapSymbol) {
      return new MapDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == eitherSymbol) {
      return new EitherDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }

    // tuple
    if (typeSymbol == tuple1Symbol) {
      return new Tuple1Deserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == tuple2Symbol) {
      return new Tuple2Deserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == tuple3Symbol) {
      return new Tuple3Deserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == tuple4Symbol) {
      return new Tuple4Deserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == tuple5Symbol) {
      return new Tuple5Deserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == tuple6Symbol) {
      return new Tuple6Deserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }

    // seq
    if (typeSymbol == listSymbol || typeSymbol == immutableListSymbol) {
      return new ListDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == vectorSymbol || typeSymbol == immutableVectorSymbol) {
      return new VectorDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == iterableSymbol || typeSymbol == immutableIterableSymbol) {
      return new IterableDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == seqSymbol || typeSymbol == immutableSeqSymbol) {
      return new SeqDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == streamSymbol || typeSymbol == immutableStreamSymbol) {
      return new StreamDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == queueSymbol) {
      return new QueueDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }

    // set
    if (typeSymbol == setSymbol || typeSymbol == immutableSetSymbol) {
      return new SetDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == hashSetSymbol) {
      return new HashSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == listSetSymbol) {
      return new ListSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == sortedSetSymbol) {
      throw new IllegalStateException("SortedSet is unsupported, because of missing ordering type class. Types: " + context.typesStackMessage)
    } else if (typeSymbol == treeSetSymbol) {
      throw new IllegalStateException("TreeSet is unsupported, because of missing ordering type class. Types: " + context.typesStackMessage)
    } else if (typeSymbol == bitSetSymbol || typeSymbol == immutableBitSetSymbol) {
      return new BitSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }

    // map
    if (typeSymbol == hashMapSymbol) {
      return new HashMapDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == sortedMapSymbol) {
      throw new IllegalStateException("SortedMap is unsupported, because of missing ordering type class. Types: " + context.typesStackMessage)
    } else if (typeSymbol == treeMapSymbol) {
      throw new IllegalStateException("TreeMap is unsupported, because of missing ordering type class. Types: " + context.typesStackMessage)
    } else if (typeSymbol == listMapSymbol) {
      return new ListMapDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }


    if(allowSuperType) {
      additionalSuperclassDeserializers.get(typeSymbol) match {
        case Some(deserializer) => return deserializer(this.asInstanceOf[DeserializerFactory])
        case None =>
          for ((tpeType, deserializer) <- additionalSuperclassDeserializers) {
            if (tpe.baseClasses.contains(tpeType)) {
              return deserializer(this.asInstanceOf[DeserializerFactory])
            }
          }
      }
    }

    if (typeSymbol == listBufferSymbol) {
      throw new IllegalArgumentException("ListBuffer is not supported, use immutable List instead. Types: " + context.typesStackMessage)
    }


    val additionalDeserializerOption = additionalDeserializers.get(tpeString)

    if (additionalDeserializerOption.isDefined) {
      additionalDeserializerOption.get(this.asInstanceOf[DeserializerFactory])
    } else if (nothingSymbol == typeSymbol) {
      throw new IllegalArgumentException("Deserialization of 'Nothing' type is not supported, be sure to define types everywhere. Types: " + context.typesStackMessage)
    } else if (anySymbol == typeSymbol) {
      throw new IllegalArgumentException("Deserialization of 'Any' or wildcard '_' type is not supported, be sure to define type more precisely. Types: " + context.typesStackMessage)
    } else if (anyRefSymbol == typeSymbol) {
      throw new IllegalArgumentException("Deserialization of 'AnyRef' type is not supported, be sure to define type more precisely. Types: " + context.typesStackMessage)
    } else if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.field.getName == "MODULE$")) {
      new SingletonObjectDeserializer(tpe)
    } else {
      new BeanDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context, ignoreNonExistingFields)
    }
  }

}
