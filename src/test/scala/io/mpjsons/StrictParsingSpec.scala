package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import java.time.LocalDate

object StrictParsingSpec {
  case class Pair(a: Int, b: String)
  case class Holder(xs: List[Int], m: Map[String, Int], o: Option[Int], d: LocalDate)
}

/** MPJsonsConfig(strict = true): standard JSON structure is enforced; the default stays lenient. */
class StrictParsingSpec extends AnyFlatSpec {
  import StrictParsingSpec._

  val lenient = new MPJsons
  val strict = new MPJsons(MPJsonsConfig(strict = true))
  val lenientMapsAsObjects = new MPJsons(MPJsonsConfig(mapsAsObjects = true))
  val strictMapsAsObjects = new MPJsons(MPJsonsConfig(strict = true, mapsAsObjects = true))

  "lenient parsing" must "keep accepting missing commas, trailing commas and trailing content" in {
    lenient.deserialize[Pair]("""{"a":1 "b":"x"}""") mustBe Pair(1, "x")
    lenient.deserialize[Pair]("""{"a":1,"b":"x",}""") mustBe Pair(1, "x")
    lenient.deserialize[Pair]("""{"a":1,"b":"x"} trailing""") mustBe Pair(1, "x")
    lenient.deserialize[List[Int]]("[1 2,3,]") mustBe List(1, 2, 3)
    lenient.deserialize[Map[String, Int]]("""[["a",1] ["b",2],]""") mustBe Map("a" -> 1, "b" -> 2)
  }

  "strict parsing" must "accept well-formed JSON, whitespace and bare keys" in {
    val holder = Holder(List(1, 2), Map("k" -> 1), Some(3), LocalDate.of(2023, 7, 23))
    strict.deserialize[Holder](strict.serialize(holder)) mustBe holder
    strict.deserialize[Holder](lenient.serialize(holder)) mustBe holder
    strictMapsAsObjects.deserialize[Holder](lenientMapsAsObjects.serialize(holder)) mustBe holder
    strict.deserialize[Pair](""" { "a" : 1 , "b" : "x" } """) mustBe Pair(1, "x")
    strict.deserialize[Pair]("""{ a : 1 , b : "x" }""") mustBe Pair(1, "x")
    strict.deserialize[List[Int]](" [ ] ") mustBe Nil
    strict.deserialize[Pair]("{}") mustBe Pair(0, null)
    strict.deserialize[Int](" 42 ") mustBe 42
    strict.deserialize[String]("\"s\"") mustBe "s"
    strict.deserialize[Option[Int]]("{}") mustBe None
    strict.buildStaticDeserializer[Pair].deserialize("""{"a":1,"b":"x"}""") mustBe Pair(1, "x")
  }

  "strict parsing" must "reject a missing comma" in {
    intercept[JsonInnerException] { strict.deserialize[Pair]("""{"a":1 "b":"x"}""") }.getMessage must include("Expected ',' or '}'")
    intercept[JsonInnerException] { strict.deserialize[List[Int]]("[1 2]") }.getMessage must include("Expected ',' or ']'")
    intercept[JsonInnerException] { strict.deserialize[Map[String, Int]]("""[["a",1] ["b",2]]""") }
    intercept[JsonInnerException] { strictMapsAsObjects.deserialize[Map[String, Int]]("""{"a":1 "b":2}""") }
    intercept[JsonInnerException] { strict.deserialize[LocalDate]("""{"year":2023 "month":7,"day":23}""") }
    intercept[JsonInnerException] { new MPJsons(MPJsonsConfig(strict = true, ignoreNonExistingFields = true)).deserialize[Pair]("""{"zzz":{"q":1 "w":2},"a":1,"b":"x"}""") }
  }

  "strict parsing" must "reject a trailing comma" in {
    intercept[JsonInnerException] { strict.deserialize[Pair]("""{"a":1,"b":"x",}""") }.getMessage must include("Trailing ','")
    intercept[JsonInnerException] { strict.deserialize[List[Int]]("[1,2,]") }.getMessage must include("Trailing ','")
    intercept[JsonInnerException] { strict.deserialize[Map[String, Int]]("""[["a",1],]""") }
  }

  "strict parsing" must "reject content after the root value" in {
    intercept[JsonInnerException] { strict.deserialize[Pair]("""{"a":1,"b":"x"} x""") }.getMessage must include("Unexpected content after the JSON value")
    intercept[JsonInnerException] { strict.deserialize[Int]("42 43") }
    intercept[JsonInnerException] { strict.deserialize[List[Int]]("[1]]") }
    intercept[JsonInnerException] { strict.buildStaticDeserializer[Pair].deserialize("""{"a":1,"b":"x"}}""") }
    strict.deserialize[Pair]("""{"a":1,"b":"x"}   """) mustBe Pair(1, "x")
  }
}
