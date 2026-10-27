package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object TypeNameSpec {
  sealed trait Shape
  @typeName("c") case class Circle(r: Double) extends Shape
  @typeName("s") case class Square(side: Int) extends Shape
  case class Triangle(a: Int) extends Shape
  @typeName("p") case object Point extends Shape
  @typeName("q\"x") case class Quoted(v: Int) extends Shape

  // Kitten was renamed from Cat; the alias keeps the old name on the wire.
  sealed trait Animal
  @typeName("Cat") case class Kitten(name: String) extends Animal
  case class Dog(name: String) extends Animal

  sealed trait Clash
  @typeName("x") case class C1(a: Int) extends Clash
  @typeName("x") case class C2(a: Int) extends Clash

  sealed trait Steal
  @typeName("B") case class A1(a: Int) extends Steal
  case class B(b: Int) extends Steal

  sealed abstract class Vehicle
  @typeName("car") case class Car(seats: Int) extends Vehicle
  case class Bike(gears: Int) extends Vehicle

  case class Drawing(shapes: List[Shape], main: Shape)

  trait NestedOpen
  @typeName("no") case class AliasedNested(a: Int) extends NestedOpen
  @typeName("np") case object NestedPoint extends NestedOpen
  case class PlainNested(a: Int) extends NestedOpen

  trait ClashOpen
  @typeName("dup") case class DupA(a: Int) extends ClashOpen
  @typeName("dup") case class DupB(a: Int) extends ClashOpen
}

// Non-sealed hierarchy: Scala records no subclasses, so aliases are resolved by listing the package on the class path.
// FarOpen (package io.mpjsons.elsewhere) is not a sibling, so its alias cannot be resolved.
trait OpenShape
@typeName("o") case class AliasedOpen(a: Int) extends OpenShape
case class PlainOpen(a: Int) extends OpenShape

class TypeNameSpec extends AnyFlatSpec {
  import TypeNameSpec._

  val mpjsons = new MPJsons
  mpjsons.markTypedClass[Shape]
  mpjsons.markTypedClass[Animal]

  "@typeName" must "replace the simple class name in the type wrapper" in {
    val shapes: List[Shape] = List(Circle(1.5), Square(2), Triangle(3), Point, Quoted(4))
    val json = mpjsons.serialize(shapes)
    json mustBe """[{"c":{"r":1.5}},{"s":{"side":2}},{"Triangle":{"a":3}},{"p":{}},{"q\"x":{"v":4}}]"""
    mpjsons.deserialize[List[Shape]](json) mustBe shapes

    val drawing = Drawing(List(Circle(1.0)), Point)
    mpjsons.deserialize[Drawing](mpjsons.serialize(drawing)) mustBe drawing
  }

  "@typeName" must "still accept the simple class name when reading" in {
    mpjsons.deserialize[List[Shape]]("""[{"Circle":{"r":1.5}},{"Square":{"side":2}},{"Point$":{}}]""") mustBe List(Circle(1.5), Square(2), Point)
  }

  "@typeName" must "keep old data readable after a class is renamed" in {
    mpjsons.serialize[Animal](Kitten("Tom")) mustBe """{"Cat":{"name":"Tom"}}"""
    mpjsons.deserialize[Animal]("""{"Cat":{"name":"Tom"}}""") mustBe Kitten("Tom")
    mpjsons.deserialize[Animal]("""{"Kitten":{"name":"Tom"}}""") mustBe Kitten("Tom")
    mpjsons.deserialize[Animal]("""{"Dog":{"name":"Rex"}}""") mustBe Dog("Rex")
  }

  "@typeName" must "reject two subclasses that would write the same name" in {
    val clash = intercept[IllegalArgumentException] { new MPJsons().markTypedClass[Clash] }
    clash.getMessage must include("'x'")
    val steal = intercept[IllegalArgumentException] { new MPJsons().markTypedClass[Steal] }
    steal.getMessage must include("'B'")
  }

  "@typeName" must "work for sealed abstract classes and markTypedSuperClass" in {
    val m = new MPJsons
    m.markTypedSuperClass[Vehicle]
    val vehicles: List[Vehicle] = List(Car(4), Bike(21))
    m.serialize(vehicles) mustBe """[{"car":{"seats":4}},{"Bike":{"gears":21}}]"""
    m.deserialize[List[Vehicle]]("""[{"car":{"seats":4}},{"Bike":{"gears":21}}]""") mustBe vehicles
  }

  "@typeName" must "work for subclasses of a non-sealed trait in its package, found on the class path" in {
    val m = new MPJsons
    m.markTypedClass[OpenShape]
    m.serialize[OpenShape](PlainOpen(1)) mustBe """{"PlainOpen":{"a":1}}"""
    m.deserialize[OpenShape]("""{"PlainOpen":{"a":1}}""") mustBe PlainOpen(1)
    m.serialize[OpenShape](AliasedOpen(1)) mustBe """{"o":{"a":1}}"""
    m.deserialize[OpenShape]("""{"o":{"a":1}}""") mustBe AliasedOpen(1)
    m.deserialize[OpenShape]("""{"AliasedOpen":{"a":1}}""") mustBe AliasedOpen(1)
    // a reader that never wrote the alias
    val reader = new MPJsons
    reader.markTypedClass[OpenShape]
    reader.deserialize[List[OpenShape]]("""[{"o":{"a":1}},{"PlainOpen":{"a":2}}]""") mustBe List(AliasedOpen(1), PlainOpen(2))
  }

  "@typeName" must "work for a non-sealed trait nested in an object, including case objects" in {
    val m = new MPJsons
    m.markTypedClass[NestedOpen]
    val values: List[NestedOpen] = List(AliasedNested(1), NestedPoint, PlainNested(2))
    val json = m.serialize(values)
    json mustBe """[{"no":{"a":1}},{"np":{}},{"PlainNested":{"a":2}}]"""
    m.deserialize[List[NestedOpen]](json) mustBe values
    val reader = new MPJsons
    reader.markTypedClass[NestedOpen]
    reader.deserialize[List[NestedOpen]](json) mustBe values
  }

  "@typeName" must "refuse to write an alias that the reader could not resolve" in {
    val m = new MPJsons
    m.markTypedClass[OpenShape]
    val e = intercept[IllegalArgumentException] { m.serialize[OpenShape](io.mpjsons.elsewhere.FarOpen(1)) }
    e.getMessage must include("FarOpen")
    e.getMessage must include("package")
    intercept[JsonInnerException] { m.deserialize[OpenShape]("""{"far":{"a":1}}""") }.getMessage must include("Unknown type name 'far'")
  }

  "Listing a package for aliases" must "find classes in jars as well as in directories" in {
    // scala-library is a jar: the concrete subclasses of AbstractMap in its package, nested ones included
    val table = new io.mpjsons.impl.special.TypeNames.Siblings(classOf[scala.collection.immutable.AbstractMap[_, _]]).table
    table.get("HashMap") mustBe Some(classOf[scala.collection.immutable.HashMap[_, _]])
    table.get("ListMap") mustBe Some(classOf[scala.collection.immutable.ListMap[_, _]])
    table.get("Map1") mustBe Some(classOf[scala.collection.immutable.Map.Map1[_, _]])
  }

  "@typeName" must "reject siblings of a non-sealed trait that write the same name" in {
    val m = new MPJsons
    m.markTypedClass[ClashOpen]
    val e = intercept[IllegalArgumentException] { m.serialize[ClashOpen](DupA(1)) }
    e.getMessage must include("'dup'")
  }

  "@typeName" must "report unknown names with the known ones" in {
    val e = intercept[JsonInnerException] { mpjsons.deserialize[Shape]("""{"Hexagon":{}}""") }
    e.getMessage must include("Hexagon")
    e.getMessage must include("c, ")
  }
}
