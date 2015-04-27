package io.mpjsons.impl.factoryimpl

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.DeserializerFactory
import io.mpjsons.impl.deserializer.immutables._
import io.mpjsons.impl.deserializer.mutables.ArrayDeserializer
import io.mpjsons.impl.deserializer.utiltypes.{EitherDeserializer, Tuple2Deserializer}
import io.mpjsons.impl.deserializer.values._
import io.mpjsons.impl.deserializer.{BeanDeserializer, SingletonObjectDeserializer}
import io.mpjsons.impl.util.reflection.ReflectionUtil

import scala.collection._
import scala.collection.mutable.ListBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryImpl {

  private var additionalDeserializers = Map[Type, DeserializerFactory => JsonTypeDeserializer[_]]()
  private var additionalSuperclassDeserializers = Map[Symbol, DeserializerFactory => JsonTypeDeserializer[_]]()

  def registerDeserializer[T](tpe: Type, deserializer: DeserializerFactory => JsonTypeDeserializer[T]) {
    additionalDeserializers += tpe -> deserializer
  }

  def registerSuperclassDeserializer[T](tpe: Type, deserializer: DeserializerFactory => JsonTypeDeserializer[T]) {
    additionalSuperclassDeserializers += tpe.typeSymbol -> deserializer
  }

  protected def getDeserializerNoCache(tpe: Type): JsonTypeDeserializer[_ <: Any] = {

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
    } else if (tpe.asInstanceOf[TypeRef].sym == definitions.ArrayClass) {
      return new ArrayDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[(_, _)].typeSymbol) {
      return new Tuple2Deserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[Option[_]].typeSymbol) {
      return new OptionDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[Map[_, _]].typeSymbol || typeSymbol == typeOf[immutable.Map[_, _]].typeSymbol) {
      return new MapDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[Either[_, _]].typeSymbol) {
      return new EitherDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    }

    // seq
    if (typeSymbol == typeOf[List[_]].typeSymbol || typeSymbol == typeOf[immutable.List[_]].typeSymbol) {
      return new ListDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[Vector[_]].typeSymbol || typeSymbol == typeOf[immutable.Vector[_]].typeSymbol) {
      return new VectorDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[Seq[_]].typeSymbol || typeSymbol == typeOf[immutable.Seq[_]].typeSymbol) {
      return new SeqDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[Stream[_]].typeSymbol || typeSymbol == typeOf[immutable.Stream[_]].typeSymbol) {
      return new StreamDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[immutable.Queue[_]].typeSymbol) {
      return new QueueDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    }

    // set
    if (typeSymbol == typeOf[Set[_]].typeSymbol || typeSymbol == typeOf[immutable.Set[_]].typeSymbol) {
      return new SetDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[immutable.HashSet[_]].typeSymbol) {
      return new HashSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[immutable.ListSet[_]].typeSymbol) {
      return new ListSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[immutable.SortedSet[_]].typeSymbol) {// Unsupported because of missing ordering type class
     // return SortedSetDeserializer
      throw new IllegalStateException("SortedSet is unsupported, because of missing ordering type class")
    } else if (typeSymbol == typeOf[immutable.TreeSet[_]].typeSymbol) {
     // return TreeSetDeserializer
      throw new IllegalStateException("TreeSet is unsupported, because of missing ordering type class")
    } else if (typeSymbol == typeOf[BitSet].typeSymbol || typeSymbol == typeOf[immutable.BitSet].typeSymbol) {
      return new BitSetDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    }

    // map
    if (typeSymbol == typeOf[Map[_, _]].typeSymbol) {
      return new MapDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[immutable.HashMap[_, _]].typeSymbol) {
      return new HashMapDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    } else if (typeSymbol == typeOf[immutable.SortedMap[_,_]].typeSymbol) {
      throw new IllegalStateException("SortedMap is unsupported, because of missing ordering type class")
      //return SortedMapDeserializer
    } else if (typeSymbol == typeOf[immutable.TreeMap[_,_]].typeSymbol) {
      throw new IllegalStateException("TreeMap is unsupported, because of missing ordering type class")
      //return TreeMapDeserializer
    } else if (typeSymbol == typeOf[immutable.ListMap[_, _]].typeSymbol) {
      return new ListMapDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    }



    additionalSuperclassDeserializers.get(typeSymbol) match {
      case Some(deserializer) => return deserializer(this.asInstanceOf[DeserializerFactory])
      case None =>
        for ((tpeType, deserializer) <- additionalSuperclassDeserializers) {
          if (tpe.baseClasses.contains(tpeType)) {
            return deserializer(this.asInstanceOf[DeserializerFactory])
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
      additionalDeserializerOption.get(this.asInstanceOf[DeserializerFactory])
    } else {
      new BeanDeserializer(this.asInstanceOf[DeserializerFactory], tpe)
    }
  }

}
