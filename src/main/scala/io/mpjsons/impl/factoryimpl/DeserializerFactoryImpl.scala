package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.DeserializerFactory
import io.mpjsons.impl.deserializer.immutables._
import io.mpjsons.impl.deserializer.mutables.ArrayDeserializer
import io.mpjsons.impl.deserializer.time.{LocalDateDeserializer, LocalDateTimeDeserializer, LocalTimeDeserializer}
import io.mpjsons.impl.deserializer.utiltypes.{EitherDeserializer, Tuple2Deserializer}
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

class DeserializerFactoryImpl(ignoreNonExistingFields: Boolean) {

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

    if (typeSymbol == typeOf[Long].typeSymbol) {
      return LongDeserializer
    } else if (typeSymbol == typeOf[Int].typeSymbol) {
      return IntDeserializer
    } else if (typeSymbol == typeOf[Boolean].typeSymbol) {
      return BooleanDeserializer
    } else if (typeSymbol == typeOf[String].typeSymbol) {
      return StringDeserializer
    } else if (typeSymbol == typeOf[Double].typeSymbol) {
      return DoubleDeserializer
    } else if (typeSymbol == typeOf[Float].typeSymbol) {
      return FloatDeserializer
    } else if (typeSymbol == typeOf[Short].typeSymbol) {
      return ShortDeserializer
    } else if (typeSymbol == typeOf[Byte].typeSymbol) {
      return ByteDeserializer
    } else if (typeSymbol == typeOf[Char].typeSymbol) {
      return CharDeserializer
    } else if (typeSymbol == typeOf[java.lang.Long].typeSymbol) {
      return LongDeserializer
    } else if (typeSymbol == typeOf[java.lang.Integer].typeSymbol) {
      return IntDeserializer
    } else if (typeSymbol == typeOf[java.lang.Boolean].typeSymbol) {
      return BooleanDeserializer
    } else if (typeSymbol == typeOf[java.lang.Double].typeSymbol) {
      return DoubleDeserializer
    } else if (typeSymbol == typeOf[java.lang.Float].typeSymbol) {
      return FloatDeserializer
    } else if (typeSymbol == typeOf[java.lang.Short].typeSymbol) {
      return ShortDeserializer
    } else if (typeSymbol == typeOf[java.lang.Byte].typeSymbol) {
      return ByteDeserializer
    } else if (typeSymbol == typeOf[java.time.LocalTime].typeSymbol) {
      return LocalTimeDeserializer
    } else if (typeSymbol == typeOf[java.time.LocalDate].typeSymbol) {
      return LocalDateDeserializer
    } else if (typeSymbol == typeOf[java.time.LocalDateTime].typeSymbol) {
      return LocalDateTimeDeserializer
    } else if (typeSymbol == typeOf[java.lang.Character].typeSymbol) {
      return CharDeserializer
    } else if (tpe.asInstanceOf[TypeRef].sym == definitions.ArrayClass) {
      return new ArrayDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[(_, _)].typeSymbol) {
      return new Tuple2Deserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[Option[_]].typeSymbol) {
      return new OptionDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[Map[_, _]].typeSymbol || typeSymbol == typeOf[immutable.Map[_, _]].typeSymbol) {
      return new MapDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[Either[_, _]].typeSymbol) {
      return new EitherDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }

    // seq
    if (typeSymbol == typeOf[List[_]].typeSymbol || typeSymbol == typeOf[immutable.List[_]].typeSymbol) {
      return new ListDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[Vector[_]].typeSymbol || typeSymbol == typeOf[immutable.Vector[_]].typeSymbol) {
      return new VectorDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[Iterable[_]].typeSymbol || typeSymbol == typeOf[immutable.Iterable[_]].typeSymbol) {
      return new IterableDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[Seq[_]].typeSymbol || typeSymbol == typeOf[immutable.Seq[_]].typeSymbol) {
      return new SeqDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[Stream[_]].typeSymbol || typeSymbol == typeOf[immutable.Stream[_]].typeSymbol) {
      return new StreamDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[immutable.Queue[_]].typeSymbol) {
      return new QueueDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }

    // set
    if (typeSymbol == typeOf[Set[_]].typeSymbol || typeSymbol == typeOf[immutable.Set[_]].typeSymbol) {
      return new SetDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[immutable.HashSet[_]].typeSymbol) {
      return new HashSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[immutable.ListSet[_]].typeSymbol) {
      return new ListSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[immutable.SortedSet[_]].typeSymbol) {
      // Unsupported because of missing ordering type class
      // return SortedSetDeserializer
      throw new IllegalStateException("SortedSet is unsupported, because of missing ordering type class. Types: " + context.typesStackMessage)
    } else if (typeSymbol == typeOf[immutable.TreeSet[_]].typeSymbol) {
      // return TreeSetDeserializer
      throw new IllegalStateException("TreeSet is unsupported, because of missing ordering type class. Types: " + context.typesStackMessage)
    } else if (typeSymbol == typeOf[BitSet].typeSymbol || typeSymbol == typeOf[immutable.BitSet].typeSymbol) {
      return new BitSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }

    // map
    if (typeSymbol == typeOf[Map[_, _]].typeSymbol) {
      return new MapDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[immutable.HashMap[_, _]].typeSymbol) {
      return new HashMapDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    } else if (typeSymbol == typeOf[immutable.SortedMap[_, _]].typeSymbol) {
      throw new IllegalStateException("SortedMap is unsupported, because of missing ordering type class. Types: " + context.typesStackMessage)
      //return SortedMapDeserializer
    } else if (typeSymbol == typeOf[immutable.TreeMap[_, _]].typeSymbol) {
      throw new IllegalStateException("TreeMap is unsupported, because of missing ordering type class. Types: " + context.typesStackMessage)
      //return TreeMapDeserializer
    } else if (typeSymbol == typeOf[immutable.ListMap[_, _]].typeSymbol) {
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

    if (typeSymbol == typeOf[ListBuffer[_]].typeSymbol) {
      throw new IllegalArgumentException("ListBuffer is not supported, use immutable List instead. Types: " + context.typesStackMessage)
    }


    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.field.getName == "MODULE$")) {
      return new SingletonObjectDeserializer(tpe)
    }


    val additionalDeserializerOption = additionalDeserializers.get(tpe.toString)

    if (additionalDeserializerOption.isDefined) {
      additionalDeserializerOption.get(this.asInstanceOf[DeserializerFactory])
    } else if (typeOf[Nothing].typeSymbol == typeSymbol) {
      throw new IllegalArgumentException("Deserialization of 'Nothing' type is not supported, be sure to define types everywhere. Types: " + context.typesStackMessage)
    } else if (typeOf[Any].typeSymbol == typeSymbol) {
      throw new IllegalArgumentException("Deserialization of 'Any' or wildcard '_' type is not supported, be sure to define type more precisely. Types: " + context.typesStackMessage)
    } else if (typeOf[AnyRef].typeSymbol == typeSymbol) {
      throw new IllegalArgumentException("Deserialization of 'AnyRef' type is not supported, be sure to define type more precisely. Types: " + context.typesStackMessage)
    } else {
      new BeanDeserializer(this.asInstanceOf[DeserializerFactory], tpe, context)
    }
  }

}
