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
//    if (clazz == classOf[Long]) {
//      return LongDeserializer
//    } else if (clazz == classOf[Int]) {
//      return IntDeserializer
//    } else if (clazz == classOf[Boolean]) {
//      return BooleanDeserializer
//    } else if (clazz == classOf[String]) {
//      return StringDeserializer
//    } else if (clazz == classOf[Double]) {
//      return DoubleDeserializer
//    } else if (clazz == classOf[Float]) {
//      return FloatDeserializer
//    } else if (clazz == classOf[Short]) {
//      return ShortDeserializer
//    } else if (clazz == classOf[Byte]) {
//      return ByteDeserializer
//    } else if (clazz == classOf[Char]) {
//      return CharDeserializer
//    } else if (clazz.isArray) {
//      return ArrayDeserializer
//    } else if (clazz == classOf[(_, _)]) {
//      return Tuple2Deserializer
//    } else if (clazz == classOf[Option[_]]) {
//      return OptionDeserializer
//    } else if (clazz == classOf[Map[_, _]]) {
//      return MapDeserializer
//    }
//
//    // seq
//    if (clazz == classOf[List[_]]) {
//      return ListDeserializer
//    } else if (clazz == classOf[Vector[_]]) {
//      return VectorDeserializer
//    } else if (clazz == classOf[Stream[_]]) {
//      return StreamDeserializer
//    } else if (clazz == classOf[Queue[_]]) {
//      return QueueDeserializer
//    } else if (clazz == classOf[Stack[_]]) {
//      return StackDeserializer
//    }
//
//    // set
//    if (clazz == classOf[Set[_]]) {
//      return SetDeserializer
//    } else if (clazz == classOf[HashSet[_]]) {
//      return HashSetDeserializer
//    } else if (clazz == classOf[ListSet[_]]) {
//      return ListSetDeserializer
////    } else if (clazz == classOf[SortedSet[_]]) {
////      return SortedSetDeserializer
////    } else if (clazz == classOf[TreeSet[_]]) {
////      return TreeSetDeserializer
//    } else if (clazz == classOf[BitSet]) {
//      return BitSetDeserializer
//    }
//
//    // map
//    if (clazz == classOf[Map[_,_]]) {
//      return MapDeserializer
//    } else if (clazz == classOf[HashMap[_,_]]) {
//      return HashMapDeserializer
////    } else if (clazz == classOf[SortedMap[_,_]]) {
////      return SortedMapDeserializer
////    } else if (clazz == classOf[TreeMap[_,_]]) {
////      return TreeMapDeserializer
//    } else if (clazz == classOf[ListMap[_,_]]) {
//      return ListMapDeserializer
//    }
//
//
//    for ((clazzType, deserializer) <- additionalSuperclassDeserializers) {
//      if (clazzType.isAssignableFrom(clazz)) {
//        return deserializer
//      }
//    }
//
//    if (clazz == classOf[ListBuffer[_]]) {
//      throw new IllegalArgumentException("ListBuffer is not supported, use immutable List instead")
//    }
//
//
//    if (ReflectionUtil.getAllAccessibleFields(clazz).exists(_.getName == "MODULE$")) {
//      return SingletonObjectDeserializer
//    }
//
//
//    val additionalDeserializerOption = additionalDeserializers.get(clazz)
//
//    if(additionalDeserializerOption.isDefined) {
//      additionalDeserializerOption.get
//    } else if (classOf[Class[_]].isAssignableFrom(clazz)) {
//      throw new IllegalArgumentException("Unsupported data type: Class, please provide custom deserializer for " + clazz)
//    } else {
//      BeanDeserializer
//    }
    null
  }

}
