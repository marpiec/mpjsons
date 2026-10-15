package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object NumberParsingSpec {
  case class Integers(b: Byte, s: Short, i: Int, l: Long)
  case class Decimals(d: Double, f: Float)
  case class Single(a: Int)
}

class NumberParsingSpec extends AnyFlatSpec {
  import NumberParsingSpec._

  val mpjsons = new MPJsons

  "Deserializer" must "parse integer extremes" in {
    val json = """{"b":-128,"s":32767,"i":-2147483648,"l":9223372036854775807}"""
    mpjsons.deserialize[Integers](json) mustBe Integers(-128, 32767, Int.MinValue, Long.MaxValue)
    mpjsons.deserialize[Long]("-9223372036854775808") mustBe Long.MinValue
    mpjsons.deserialize[Int]("  42  ") mustBe 42
    mpjsons.deserialize[Int]("-0") mustBe 0
    mpjsons.deserialize[List[Int]]("[1,-2, 3 ,4]") mustBe List(1, -2, 3, 4)
  }

  "Deserializer" must "report integer overflow as NumberFormatException with the offending text" in {
    val e = intercept[JsonInnerException] { mpjsons.deserialize[Single]("""{"a":99999999999}""") }
    e.getCause mustBe a[NumberFormatException]
    e.getCause.getMessage must include("99999999999")

    intercept[JsonInnerException] { mpjsons.deserialize[Long]("9223372036854775808") }.getCause mustBe a[NumberFormatException]
    intercept[JsonInnerException] { mpjsons.deserialize[Long]("-9223372036854775809") }.getCause mustBe a[NumberFormatException]
    intercept[JsonInnerException] { mpjsons.deserialize[Byte]("128") }.getCause mustBe a[NumberFormatException]
    intercept[JsonInnerException] { mpjsons.deserialize[Short]("-32769") }.getCause mustBe a[NumberFormatException]
  }

  "Deserializer" must "explain a type mismatch instead of failing on an empty number" in {
    intercept[JsonInnerException] { mpjsons.deserialize[Single]("""{"a":"1"}""") }.getMessage must include("Expected a number but found '\"'")
    intercept[JsonInnerException] { mpjsons.deserialize[Single]("""{"a":true}""") }.getMessage must include("Expected a number but found 't'")
    intercept[JsonInnerException] { mpjsons.deserialize[Single]("""{"a":-}""") }.getMessage must include("Expected a number")
    intercept[JsonInnerException] { mpjsons.deserialize[Decimals]("""{"d":"x","f":1}""") }.getMessage must include("Expected a number but found '\"'")
    intercept[JsonInnerException] { mpjsons.deserialize[Int]("") }.getMessage must include("end of input")
  }

  "Deserializer" must "parse floating point literals in every JSON form" in {
    mpjsons.deserialize[Decimals]("""{"d":1.5,"f":-2.25}""") mustBe Decimals(1.5, -2.25f)
    mpjsons.deserialize[Double]("1e-5") mustBe 1e-5
    mpjsons.deserialize[Double]("1E5") mustBe 1e5
    mpjsons.deserialize[Double]("-0.0") mustBe -0.0
    mpjsons.deserialize[Double]("123456789012345678") mustBe 123456789012345678L.toDouble
    mpjsons.deserialize[Double]("1234567890123456789") mustBe 1234567890123456789L.toDouble
    mpjsons.deserialize[Double]("0.1") mustBe 0.1
    mpjsons.deserialize[Float]("0.1") mustBe 0.1f
    mpjsons.deserialize[Float]("3.4028235E38") mustBe Float.MaxValue
    mpjsons.deserialize[Double]("42") mustBe 42.0
    mpjsons.deserialize[Float]("-7") mustBe -7f
  }

  "Deserializer" must "reject malformed floating point literals" in {
    intercept[JsonInnerException] { mpjsons.deserialize[Double]("1.2.3") }.getCause mustBe a[NumberFormatException]
    intercept[JsonInnerException] { mpjsons.deserialize[Double]("--1") }.getCause mustBe a[NumberFormatException]
  }

  "Serializer" must "round trip all numeric types" in {
    val ints = Integers(1, -2, 3, -4L)
    mpjsons.deserialize[Integers](mpjsons.serialize(ints)) mustBe ints
    val decimals = Decimals(1.0e300, 1.5e-10f)
    mpjsons.deserialize[Decimals](mpjsons.serialize(decimals)) mustBe decimals
  }
}
