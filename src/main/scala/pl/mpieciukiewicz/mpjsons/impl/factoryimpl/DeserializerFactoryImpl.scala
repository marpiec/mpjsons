package pl.mpieciukiewicz.mpjsons.impl.factoryimpl

import pl.mpieciukiewicz.mpjsons.impl.serializer.BeanSerializer
import pl.mpieciukiewicz.mpjsons.impl.util.reflection.ReflectionUtil

import collection.mutable.ListBuffer

import pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives._
import pl.mpieciukiewicz.mpjsons.impl.deserializer._
import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array._
import scala.collection.immutable._
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set._
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq._
import pl.mpieciukiewicz.mpjsons.impl.deserializer.map.{ListMapDeserializer, MapDeserializer, HashMapDeserializer}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactoryImpl {

  private var additionalDeserializers: Map[Type, JsonTypeDeserializer[_ <: Any]] = Map[Type, JsonTypeDeserializer[Any]]()
  private var additionalSuperclassDeserializers: Map[Type, JsonTypeDeserializer[_ <: Any]] = Map[Type, JsonTypeDeserializer[Any]]()

  def registerDeserializer(tpe: Type, deserializer: JsonTypeDeserializer[_ <: Any]) {
    additionalDeserializers += tpe -> deserializer
  }

  def registerSuperclassDeserializer(tpe: Type, deserializer: JsonTypeDeserializer[_ <: Any]) {
    additionalSuperclassDeserializers += tpe -> deserializer
  }

  def getDeserializer(tpe: Type): JsonTypeDeserializer[_ <: Any] = {

    if (tpe == typeOf[Long]) {
      return LongDeserializer
    } else if (tpe == typeOf[Int]) {
      return IntDeserializer
    } else if (tpe == typeOf[Boolean]) {
      return BooleanDeserializer
    } else if (tpe == typeOf[String]) {
      return StringDeserializer
    } else if (tpe == typeOf[Double]) {
      return DoubleDeserializer
    } else if (tpe == typeOf[Float]) {
      return FloatDeserializer
    } else if (tpe == typeOf[Short]) {
      return ShortDeserializer
    } else if (tpe == typeOf[Byte]) {
      return ByteDeserializer
    } else if (tpe == typeOf[Char]) {
      return CharDeserializer
//    } else if (tpe.isArray) { TODO fix
//      return ArrayDeserializer
    } else if (tpe == typeOf[(_, _)]) {
      return Tuple2Deserializer
    } else if (tpe == typeOf[Option[_]]) {
      return OptionDeserializer
    } else if (tpe == typeOf[Map[_, _]]) {
      return MapDeserializer
    }

    // seq
    if (tpe == typeOf[List[_]]) {
      return ListDeserializer
    } else if (tpe == typeOf[Vector[_]]) {
      return VectorDeserializer
    } else if (tpe == typeOf[Stream[_]]) {
      return StreamDeserializer
    } else if (tpe == typeOf[Queue[_]]) {
      return QueueDeserializer
    } else if (tpe == typeOf[Stack[_]]) {
      return StackDeserializer
    }

    // set
    if (tpe == typeOf[Set[_]]) {
      return SetDeserializer
    } else if (tpe == typeOf[HashSet[_]]) {
      return HashSetDeserializer
    } else if (tpe == typeOf[ListSet[_]]) {
      return ListSetDeserializer
//    } else if (tpe == typeOf[SortedSet[_]]) {
//      return SortedSetDeserializer
//    } else if (tpe == typeOf[TreeSet[_]]) {
//      return TreeSetDeserializer
    } else if (tpe == typeOf[BitSet]) {
      return BitSetDeserializer
    }

    // map
    if (tpe == typeOf[Map[_,_]]) {
      return MapDeserializer
    } else if (tpe == typeOf[HashMap[_,_]]) {
      return HashMapDeserializer
//    } else if (tpe == typeOf[SortedMap[_,_]]) {
//      return SortedMapDeserializer
//    } else if (tpe == typeOf[TreeMap[_,_]]) {
//      return TreeMapDeserializer
    } else if (tpe == typeOf[ListMap[_,_]]) {
      return ListMapDeserializer
    }


    for ((tpeType, deserializer) <- additionalSuperclassDeserializers) {
//      if (tpeType.isAssignableFrom(tpe)) { //TODO fix
//        return deserializer
//      }
    }

    if (tpe == typeOf[ListBuffer[_]]) {
      throw new IllegalArgumentException("ListBuffer is not supported, use immutable List instead")
    }


    if (ReflectionUtil.getAllAccessibleFields(tpe).exists(_.getName == "MODULE$")) {
      return SingletonObjectDeserializer
    }


    val additionalDeserializerOption = additionalDeserializers.get(tpe)

    if(additionalDeserializerOption.isDefined) {
      additionalDeserializerOption.get
//    } else if (typeOf[Class[_]].isAssignableFrom(tpe)) { // TODO fix
//      throw new IllegalArgumentException("Unsupported data type: Class, please provide custom deserializer for " + tpe)
    } else {
      BeanDeserializer
    }
  }

}
