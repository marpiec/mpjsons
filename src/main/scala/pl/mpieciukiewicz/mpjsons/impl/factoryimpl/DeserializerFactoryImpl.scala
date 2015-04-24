package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.{MPJsons, JsonTypeDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer._
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array._
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq._
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set._
import pl.mpieciukiewicz.mpjsons.impl.deserializer.map.{HashMapDeserializer, ListMapDeserializer, MapDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives._
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil

import scala.collection.immutable._
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
      return new OptionDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Map[_, _]].typeSymbol) {
      return new MapDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Either[_, _]].typeSymbol) {
      return new EitherDeserializer(deserializerFactory, tpe)
    }

    // seq
    if (typeSymbol == typeOf[List[_]].typeSymbol) {
      return new ListDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Vector[_]].typeSymbol) {
      return new VectorDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Seq[_]].typeSymbol) {
      return new SeqDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Stream[_]].typeSymbol) {
      return new StreamDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[Queue[_]].typeSymbol) {
      return new QueueDeserializer(deserializerFactory, tpe)
    }

    // set
    if (typeSymbol == typeOf[Set[_]].typeSymbol) {
      return new SetDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[HashSet[_]].typeSymbol) {
      return new HashSetDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[ListSet[_]].typeSymbol) {
      return new ListSetDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[SortedSet[_]].typeSymbol) {// Unsupported because of missing ordering type class
     // return SortedSetDeserializer
      throw new IllegalStateException("SortedSet is unsupported, because of missing ordering type class")
    } else if (typeSymbol == typeOf[TreeSet[_]].typeSymbol) {
     // return TreeSetDeserializer
      throw new IllegalStateException("TreeSet is unsupported, because of missing ordering type class")
    } else if (typeSymbol == typeOf[BitSet].typeSymbol) {
      return new BitSetDeserializer(deserializerFactory, tpe)
    }

    // map
    if (typeSymbol == typeOf[Map[_, _]].typeSymbol) {
      return new MapDeserializer(deserializerFactory, tpe)
    } else if (typeSymbol == typeOf[HashMap[_, _]].typeSymbol) {
      return new HashMapDeserializer(deserializerFactory, tpe)
    } else if (tpe == typeOf[SortedMap[_,_]].typeSymbol) {
      throw new IllegalStateException("SortedMap is unsupported, because of missing ordering type class")
      //return SortedMapDeserializer
    } else if (tpe == typeOf[TreeMap[_,_]].typeSymbol) {
      throw new IllegalStateException("TreeMap is unsupported, because of missing ordering type class")
      //return TreeMapDeserializer
    } else if (typeSymbol == typeOf[ListMap[_, _]].typeSymbol) {
      return new ListMapDeserializer(deserializerFactory, tpe)
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
