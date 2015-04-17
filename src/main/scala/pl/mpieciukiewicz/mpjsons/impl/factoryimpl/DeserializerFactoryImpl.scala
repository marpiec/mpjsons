package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
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

    if (tpe.typeSymbol == typeOf[Long].typeSymbol) {
      return LongDeserializer
    } else if (tpe.typeSymbol == typeOf[Int].typeSymbol) {
      return IntDeserializer
    } else if (tpe.typeSymbol == typeOf[Boolean].typeSymbol) {
      return BooleanDeserializer
    } else if (tpe.typeSymbol == typeOf[String].typeSymbol) {
      return StringDeserializer
    } else if (tpe.typeSymbol == typeOf[Double].typeSymbol) {
      return DoubleDeserializer
    } else if (tpe.typeSymbol == typeOf[Float].typeSymbol) {
      return FloatDeserializer
    } else if (tpe.typeSymbol == typeOf[Short].typeSymbol) {
      return ShortDeserializer
    } else if (tpe.typeSymbol == typeOf[Byte].typeSymbol) {
      return ByteDeserializer
    } else if (tpe.typeSymbol == typeOf[Char].typeSymbol) {
      return CharDeserializer
    } else if (tpe.asInstanceOf[TypeRef].sym == definitions.ArrayClass) {
      return ArrayDeserializer
    } else if (tpe.typeSymbol == typeOf[(_, _)].typeSymbol) {
      return Tuple2Deserializer
    } else if (tpe.typeSymbol == typeOf[Option[_]].typeSymbol) {
      return OptionDeserializer
    } else if (tpe.typeSymbol == typeOf[Map[_, _]].typeSymbol) {
      return MapDeserializer
    }

    // seq
    if (tpe.typeSymbol == typeOf[List[_]].typeSymbol) {
      return ListDeserializer
    } else if (tpe.typeSymbol == typeOf[Vector[_]].typeSymbol) {
      return VectorDeserializer
    } else if (tpe.typeSymbol == typeOf[Stream[_]].typeSymbol) {
      return StreamDeserializer
    } else if (tpe.typeSymbol == typeOf[Queue[_]].typeSymbol) {
      return QueueDeserializer
    }

    // set
    if (tpe.typeSymbol == typeOf[Set[_]].typeSymbol) {
      return SetDeserializer
    } else if (tpe.typeSymbol == typeOf[HashSet[_]].typeSymbol) {
      return HashSetDeserializer
    } else if (tpe.typeSymbol == typeOf[ListSet[_]].typeSymbol) {
      return ListSetDeserializer
      //    } else if (tpe == typeOf[SortedSet[_]]) {
      //      return SortedSetDeserializer
      //    } else if (tpe == typeOf[TreeSet[_]]) {
      //      return TreeSetDeserializer
    } else if (tpe.typeSymbol == typeOf[BitSet].typeSymbol) {
      return BitSetDeserializer
    }

    // map
    if (tpe.typeSymbol == typeOf[Map[_, _]].typeSymbol) {
      return MapDeserializer
    } else if (tpe.typeSymbol == typeOf[HashMap[_, _]].typeSymbol) {
      return HashMapDeserializer
      //    } else if (tpe == typeOf[SortedMap[_,_]]) {
      //      return SortedMapDeserializer
      //    } else if (tpe == typeOf[TreeMap[_,_]]) {
      //      return TreeMapDeserializer
    } else if (tpe.typeSymbol == typeOf[ListMap[_, _]].typeSymbol) {
      return ListMapDeserializer
    }



    additionalSuperclassDeserializers.get(tpe.typeSymbol) match {
      case Some(deserializer) => return deserializer
      case None =>
        for ((tpeType, deserializer) <- additionalSuperclassDeserializers) {
          if (tpe.baseClasses.contains(tpeType)) {
            return deserializer
          }
        }
    }

    if (tpe.typeSymbol == typeOf[ListBuffer[_]].typeSymbol) {
      throw new IllegalArgumentException("ListBuffer is not supported, use immutable List instead")
    }


    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.getName == "MODULE$")) {
      return SingletonObjectDeserializer
    }


    val additionalDeserializerOption = additionalDeserializers.get(tpe)

    if (additionalDeserializerOption.isDefined) {
      additionalDeserializerOption.get
    } else {
      BeanDeserializer
    }
  }

}
