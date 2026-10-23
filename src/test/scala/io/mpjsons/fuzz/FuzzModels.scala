package io.mpjsons.fuzz

import io.mpjsons.nullable

import scala.annotation.meta.field
import scala.collection.immutable.{BitSet, HashMap, HashSet, ListMap, ListSet, Queue}

case class Prims(b: Byte, s: Short, i: Int, l: Long, f: Float, d: Double, c: Char, z: Boolean, str: String)

case class Boxed(@(nullable @field) a: java.lang.Long,
                 @(nullable @field) b: java.lang.Integer,
                 @(nullable @field) c: java.lang.Short,
                 @(nullable @field) d: java.lang.Byte,
                 @(nullable @field) e: java.lang.Boolean,
                 @(nullable @field) f: java.lang.Double,
                 @(nullable @field) g: java.lang.Float,
                 @(nullable @field) h: java.lang.Character,
                 @(nullable @field) s: String)

case class Inner(a: Int, b: String)

case class Colls(list: List[Int], vector: Vector[String], seq: Seq[Long], iterable: Iterable[Double], set: Set[String],
                 hashSet: HashSet[Int], listSet: ListSet[String], bitSet: BitSet, queue: Queue[Int], stream: Stream[Int],
                 nested: List[List[Inner]])

case class Maps(map: Map[String, Int], hashMap: HashMap[Int, String], listMap: ListMap[String, Long],
                objKeys: Map[Inner, List[Inner]])

case class Arrays(ints: Array[Int], longs: Array[Long], doubles: Array[Double], strings: Array[String],
                  inners: Array[Inner], bytes: Array[Byte], chars: Array[Char], bools: Array[Boolean],
                  floats: Array[Float], shorts: Array[Short], nested: Array[Array[Int]])

case class Opts(o1: Option[Int], o2: Option[String], o3: Option[Inner], o4: Option[Option[Int]],
                e1: Either[Int, String], e2: Either[Inner, List[Int]], t: (Int, String), t2: (Inner, (Long, Double)), t3: (Long, Double))

trait Animal

case class Dog(name: String, age: Int) extends Animal

case class Cat(name: String, lives: Option[Int]) extends Animal

case class Zoo(animals: List[Animal], main: Animal)

case class Tree(value: Int, children: List[Tree])

class Bean {
  var x: Int = _
  var name: String = _
  var inner: Inner = _
  var list: List[Inner] = _
  var flag: Boolean = _
}

class BeanWithFinals(var counter: Long, val label: String) {
  var extra: Double = _
}

case class Generic[T](value: T, values: List[T])

case class UsesGeneric(g: Generic[String], gi: Generic[Inner], gs: List[Generic[Inner]])
