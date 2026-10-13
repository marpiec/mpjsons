package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object ConstructorConstructionSpec {
  case class Person(name: String, age: Int, score: Double, active: Boolean, nick: Option[String], tags: List[String]) {
    var visits: Int = 0           // not a constructor parameter: written after construction
    val greeting: String = "hi " + name   // derived in the body: only correct when the constructor runs
  }
  case class Box[T](value: T, values: List[T])
  case class Validated(n: Int) {
    require(n >= 0, "n must not be negative")
  }
  case class WithNullable(@nullable a: String, b: String)
  class Plain { var x: Int = 1 }
  case class Nested(p: Person, boxes: Map[String, Box[Int]])
}

/** MPJsonsConfig(caseClassesViaConstructor = true): case classes are built through their primary constructor. */
class ConstructorConstructionSpec extends AnyFlatSpec {
  import ConstructorConstructionSpec._

  val allocating = new MPJsons
  val constructing = new MPJsons(MPJsonsConfig(caseClassesViaConstructor = true))

  "constructor mode" must "run the constructor, so body initialisers are correct" in {
    val json = """{"name":"Ann","age":3,"score":1.5,"active":true,"nick":{"value":"a"},"tags":["t"],"visits":7}"""
    val person = constructing.deserialize[Person](json)
    person mustBe Person("Ann", 3, 1.5, active = true, Some("a"), List("t"))
    person.visits mustBe 7
    person.greeting mustBe "hi Ann"
    allocating.deserialize[Person](json).greeting mustBe null // allocation mode skips the body
  }

  "constructor mode" must "use zero / false / null / None for absent keys and accept null literals" in {
    val person = constructing.deserialize[Person]("{}")
    person mustBe Person(null, 0, 0.0, active = false, None, null)
    person.visits mustBe 0
    constructing.deserialize[Person]("""{"name":null,"nick":null,"tags":null,"age":2}""") mustBe Person(null, 2, 0.0, active = false, None, null)
    intercept[JsonInnerException] { constructing.deserialize[Person]("""{"age":null}""") }.getMessage must include("null is not allowed")
  }

  "constructor mode" must "handle generics, nesting, renamed fallbacks and unknown keys like allocation mode" in {
    constructing.deserialize[Box[String]]("""{"value":"a","values":["b"]}""") mustBe Box("a", List("b"))
    constructing.deserialize[Box[Int]]("""{"value":1,"values":[2]}""") mustBe Box(1, List(2))
    val nested = Nested(Person("Bo", 1, 0.5, active = false, None, Nil), Map("k" -> Box(1, Nil)))
    constructing.deserialize[Nested](constructing.serialize(nested)) mustBe nested
    constructing.deserialize[WithNullable]("""{"b":"x"}""") mustBe WithNullable(null, "x")
    intercept[JsonInnerException] { constructing.deserialize[Person]("""{"zzz":1}""") }.getMessage must include("No field [zzz]")
    new MPJsons(MPJsonsConfig(caseClassesViaConstructor = true, ignoreNonExistingFields = true)).deserialize[Person]("""{"zzz":{"a":[1]},"age":4}""").age mustBe 4
  }

  "constructor mode" must "surface constructor validation errors" in {
    val e = intercept[JsonInnerException] { constructing.deserialize[Validated]("""{"n":-1}""") }
    e.getMessage must include("n must not be negative")
    constructing.deserialize[Validated]("""{"n":1}""") mustBe Validated(1)
    allocating.deserialize[Validated]("""{"n":-1}""").n mustBe -1 // allocation mode bypasses require
  }

  "constructor mode" must "leave plain classes on the allocation path" in {
    val plain = constructing.deserialize[Plain]("""{"x":5}""")
    plain.x mustBe 5
    constructing.deserialize[Plain]("{}").x mustBe 1
  }
}
