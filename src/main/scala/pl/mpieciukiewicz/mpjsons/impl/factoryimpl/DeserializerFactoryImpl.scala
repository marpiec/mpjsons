package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.impl.deserializer.{BeanDeserializer, SingletonObjectDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer

import pl.mpieciukiewicz.mpjsons.impl.deserializer.mutable.ArrayDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.utiltypes.{Tuple2Deserializer, EitherDeserializer}
import pl.mpieciukiewicz.mpjsons.{MPJsons, JsonTypeDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.values._
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil

import scala.collection._
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryImpl {

  private var additionalDeserializers = Map[Type, JsonTypeDeserializer[_ <: Any]]()
  private var additionalSuperclassDeserializers = Map[Symbol, JsonTypeDeserializer[_ <: Any]]()

  def registerDeserializer(tpe: Type, deserializer: JsonTypeDeserializer[_ <: Any]) {
    additionalDeserializers += tpe -> deserializer
  }

  def registerSuperclassDeserializer(tpe: Type, deserializer: JsonTypeDeserializer[_ <: Any]) {
    additionalSuperclassDeserializers += tpe.typeSymbol -> deserializer
  }

  def getDeserializer(tpe: Type): JsonTypeDeserializer[_ <: Any] = {

    val typeSymbol = tpe.typeSymbol
    val deserializerFactory = MPJsons.deserializerFactory

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
    } else if (tpe.asInstanceOf[TypeRef].sym == definitions.ArrayClass) {
      return new ArrayDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[(_, _)].typeSymbol) {
      return new Tuple2Deserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Option[_]].typeSymbol) {
      return new deserializer.immutable.OptionDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Map[_, _]].typeSymbol || typeSymbol == typeOf[immutable.Map[_, _]].typeSymbol) {
      return new deserializer.immutable.MapDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Either[_, _]].typeSymbol) {
      return new EitherDeserializer(deserializerFactory, tpe)
    }

    // seq
    if (typeSymbol == typeOf[List[_]].typeSymbol || typeSymbol == typeOf[immutable.List[_]].typeSymbol) {
      return new deserializer.immutable.ListDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Vector[_]].typeSymbol || typeSymbol == typeOf[immutable.Vector[_]].typeSymbol) {
      return new deserializer.immutable.VectorDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Seq[_]].typeSymbol || typeSymbol == typeOf[immutable.Seq[_]].typeSymbol) {
      return new deserializer.immutable.SeqDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Stream[_]].typeSymbol || typeSymbol == typeOf[immutable.Stream[_]].typeSymbol) {
      return new deserializer.immutable.StreamDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[immutable.Queue[_]].typeSymbol) {
      return new deserializer.immutable.QueueDeserializer(deserializerFactory, tpe)
    }

    // set
    if (typeSymbol == typeOf[Set[_]].typeSymbol || typeSymbol == typeOf[immutable.Set[_]].typeSymbol) {
      return new deserializer.immutable.SetDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[immutable.HashSet[_]].typeSymbol) {
      return new deserializer.immutable.HashSetDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[immutable.ListSet[_]].typeSymbol) {
      return new deserializer.immutable.ListSetDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[immutable.SortedSet[_]].typeSymbol) {// Unsupported because of missing ordering type class
     // return SortedSetDeserializer
      throw new IllegalStateException("SortedSet is unsupported, because of missing ordering type class")
    } else if (typeSymbol == typeOf[immutable.TreeSet[_]].typeSymbol) {
     // return TreeSetDeserializer
      throw new IllegalStateException("TreeSet is unsupported, because of missing ordering type class")
    } else if (typeSymbol == typeOf[BitSet].typeSymbol || typeSymbol == typeOf[immutable.BitSet].typeSymbol) {
      return new deserializer.immutable.BitSetDeserializer(deserializerFactory, tpe)
    }

    // map
    if (typeSymbol == typeOf[Map[_, _]].typeSymbol) {
      return new deserializer.immutable.MapDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[immutable.HashMap[_, _]].typeSymbol) {
      return new deserializer.immutable.HashMapDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[immutable.SortedMap[_,_]].typeSymbol) {
      throw new IllegalStateException("SortedMap is unsupported, because of missing ordering type class")
      //return SortedMapDeserializer
    } else if (typeSymbol == typeOf[immutable.TreeMap[_,_]].typeSymbol) {
      throw new IllegalStateException("TreeMap is unsupported, because of missing ordering type class")
      //return TreeMapDeserializer
    } else if (typeSymbol == typeOf[immutable.ListMap[_, _]].typeSymbol) {
      return new deserializer.immutable.ListMapDeserializer(deserializerFactory, tpe)
    }



    additionalSuperclassDeserializers.get(typeSymbol) match {
      case Some(deserializer) => return deserializer
      case None =>
        for ((tpeType, deserializer) <- additionalSuperclassDeserializers) {
          if (tpe.baseClasses.contains(tpeType)) {
            return deserializer
          }
        }
    }

    if (typeSymbol == typeOf[ListBuffer[_]].typeSymbol) {
      throw new IllegalArgumentException("ListBuffer is not supported, use immutable List instead")
    }


    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.field.getName == "MODULE$")) {
      return new SingletonObjectDeserializer(tpe)
    }


    val additionalDeserializerOption = additionalDeserializers.get(tpe)

    if (additionalDeserializerOption.isDefined) {
      additionalDeserializerOption.get
    } else {
      new BeanDeserializer(deserializerFactory, tpe)
    }
  }

}
