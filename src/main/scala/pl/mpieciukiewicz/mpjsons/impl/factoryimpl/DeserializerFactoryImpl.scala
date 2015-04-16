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
    } else if (tpe.typeSymbol == typeOf[Stack[_]].typeSymbol) {
      return StackDeserializer
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
    if (tpe.typeSymbol == typeOf[Map[_,_]].typeSymbol) {
      return MapDeserializer
    } else if (tpe.typeSymbol == typeOf[HashMap[_,_]].typeSymbol) {
      return HashMapDeserializer
//    } else if (tpe == typeOf[SortedMap[_,_]]) {
//      return SortedMapDeserializer
//    } else if (tpe == typeOf[TreeMap[_,_]]) {
//      return TreeMapDeserializer
    } else if (tpe.typeSymbol == typeOf[ListMap[_,_]].typeSymbol) {
      return ListMapDeserializer
    }


    for ((tpeType, deserializer) <- additionalSuperclassDeserializers) {
//      if (tpeType.isAssignableFrom(tpe)) { //TODO fix
//        return deserializer
//      }
    }

    if (tpe.typeSymbol == typeOf[ListBuffer[_]].typeSymbol) {
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
