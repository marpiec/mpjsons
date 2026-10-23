package io.mpjsons.fuzz

import scala.collection.immutable.{BitSet, HashMap, HashSet, ListMap, ListSet, Queue}
import scala.util.Random

/** Random value generators for fuzz models. */
class Gen(val r: Random) {

  def size(max: Int): Int = if (r.nextInt(5) == 0) 0 else r.nextInt(max + 1)

  def char(): Char = r.nextInt(10) match {
    case 0 => "\"\\/\b\f\n\r\t".charAt(r.nextInt(8))
    case 1 => (1 + r.nextInt(31)).toChar
    case 2 => (0x80 + r.nextInt(0xD000)).toChar
    case 3 => (0xE000 + r.nextInt(0x1FFF)).toChar
    case _ => (32 + r.nextInt(95)).toChar
  }

  def string(): String = r.nextInt(6) match {
    case 0 => ""
    case 1 => new String(Character.toChars(0x10000 + r.nextInt(0xFFFFF))) + word()
    case 2 => Seq.fill(1 + r.nextInt(60))(char()).mkString
    case _ => word()
  }

  def word(): String = Seq.fill(1 + r.nextInt(12))(('a' + r.nextInt(26)).toChar).mkString

  def int(): Int = r.nextInt(6) match {
    case 0 => Int.MaxValue
    case 1 => Int.MinValue
    case 2 => r.nextInt(256) - 128
    case _ => r.nextInt()
  }

  def long(): Long = r.nextInt(6) match {
    case 0 => Long.MaxValue
    case 1 => Long.MinValue
    case 2 => r.nextInt(1000).toLong
    case _ => r.nextLong()
  }

  def double(): Double = finite(rawDouble())

  def float(): Float = { val f = rawFloat(); if (f.isNaN || f.isInfinite) 1.5f else f }

  private def finite(d: Double): Double = if (d.isNaN || d.isInfinite) 2.5 else d

  private def rawDouble(): Double = r.nextInt(10) match {
    case 0 => 0.0
    case 1 => -0.0
    case 2 => Double.MaxValue
    case 3 => Double.MinPositiveValue
    case 4 => r.nextInt(100000) / 100.0
    case 5 => java.lang.Double.longBitsToDouble(r.nextLong())
    case 6 => r.nextGaussian() * math.pow(10, r.nextInt(40) - 20)
    case 7 => r.nextInt()
    case _ => r.nextDouble()
  }

  private def rawFloat(): Float = r.nextInt(6) match {
    case 0 => java.lang.Float.intBitsToFloat(r.nextInt())
    case 1 => r.nextInt(10000) / 100.0f
    case 2 => Float.MinPositiveValue
    case _ => r.nextFloat() * math.pow(10, r.nextInt(20) - 10).toFloat
  }

  def inner(): Inner = Inner(int(), string())

  def prims(): Prims = Prims(r.nextInt(256).toByte, r.nextInt(65536).toShort, int(), long(), float(), double(), char(), r.nextBoolean(), string())

  private def maybe[T <: AnyRef](v: => T): T = if (r.nextBoolean()) v else null.asInstanceOf[T]

  def boxed(): Boxed = Boxed(maybe(java.lang.Long.valueOf(long())), maybe(java.lang.Integer.valueOf(int())),
    maybe(java.lang.Short.valueOf(r.nextInt(65536).toShort)), maybe(java.lang.Byte.valueOf(r.nextInt(256).toByte)),
    maybe(java.lang.Boolean.valueOf(r.nextBoolean())), maybe(java.lang.Double.valueOf(double())),
    maybe(java.lang.Float.valueOf(float())), maybe(java.lang.Character.valueOf(char())), maybe(string()))

  def colls(): Colls = Colls(List.fill(size(6))(int()), Vector.fill(size(6))(string()), Seq.fill(size(6))(long()),
    Iterable.fill(size(6))(double()), List.fill(size(8))(word()).toSet, HashSet(List.fill(size(8))(int()): _*),
    ListSet(List.fill(size(6))(word()): _*), BitSet(List.fill(size(8))(r.nextInt(200)): _*),
    Queue(List.fill(size(5))(int()): _*), Stream.fill(size(5))(int()), List.fill(size(3))(List.fill(size(3))(inner())))

  def maps(): Maps = Maps(List.fill(size(6))(word() -> int()).toMap, HashMap(List.fill(size(6))(int() -> string()): _*),
    ListMap(List.fill(size(6))(word() -> long()): _*), List.fill(size(3))(inner() -> List.fill(size(3))(inner())).toMap)

  def arrays(): Arrays = Arrays(Array.fill(size(6))(int()), Array.fill(size(6))(long()), Array.fill(size(6))(double()),
    Array.fill(size(6))(string()), Array.fill(size(4))(inner()), Array.fill(size(6))(r.nextInt(256).toByte),
    Array.fill(size(6))(char()), Array.fill(size(6))(r.nextBoolean()), Array.fill(size(6))(float()),
    Array.fill(size(6))(r.nextInt(65536).toShort), Array.fill(size(3))(Array.fill(size(3))(int())))

  def opts(): Opts = Opts(
    if (r.nextBoolean()) Some(int()) else None,
    if (r.nextBoolean()) Some(string()) else None,
    if (r.nextBoolean()) Some(inner()) else None,
    r.nextInt(3) match { case 0 => None case 1 => Some(None) case _ => Some(Some(int())) },
    if (r.nextBoolean()) Left(int()) else Right(string()),
    if (r.nextBoolean()) Left(inner()) else Right(List.fill(size(4))(int())),
    (int(), string()), (inner(), (long(), double())), (long(), double()))

  def animal(): Animal = if (r.nextBoolean()) Dog(string(), int()) else Cat(string(), if (r.nextBoolean()) Some(int()) else None)

  def zoo(): Zoo = Zoo(List.fill(size(5))(animal()), animal())

  def tree(depth: Int = 0): Tree = Tree(int(), if (depth > 3) Nil else List.fill(size(3))(tree(depth + 1)))

  def bean(): Bean = {
    val b = new Bean
    b.x = int()
    b.name = string()
    b.inner = inner()
    b.list = List.fill(size(4))(inner())
    b.flag = r.nextBoolean()
    b
  }

  def beanWithFinals(): BeanWithFinals = {
    val b = new BeanWithFinals(long(), string())
    b.extra = double()
    b
  }

  def usesGeneric(): UsesGeneric = UsesGeneric(Generic(string(), List.fill(size(3))(string())), Generic(inner(), List.fill(size(3))(inner())), List.fill(size(3))(Generic(inner(), List.fill(size(2))(inner()))))
}
