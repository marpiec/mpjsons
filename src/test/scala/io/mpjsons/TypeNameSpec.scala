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
}

// Non-sealed hierarchy: subclasses cannot be enumerated, so aliases cannot be resolved when reading.
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

  "@typeName" must "refuse to write an alias that the reader could not resolve" in {
    val m = new MPJsons
    m.markTypedClass[OpenShape]
    m.serialize[OpenShape](PlainOpen(1)) mustBe """{"PlainOpen":{"a":1}}"""
    m.deserialize[OpenShape]("""{"PlainOpen":{"a":1}}""") mustBe PlainOpen(1)
    val e = intercept[IllegalArgumentException] { m.serialize[OpenShape](AliasedOpen(1)) }
    e.getMessage must include("sealed")
    e.getMessage must include("AliasedOpen")
  }

  "@typeName" must "report unknown names with the known ones" in {
    val e = intercept[JsonInnerException] { mpjsons.deserialize[Shape]("""{"Hexagon":{}}""") }
    e.getMessage must include("Hexagon")
    e.getMessage must include("c, ")
  }
}
