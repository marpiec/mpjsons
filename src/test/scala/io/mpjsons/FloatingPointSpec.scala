package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object FloatingPointSpec {
  case class Numbers(d: Double, f: Float, boxed: java.lang.Double)
}

class FloatingPointSpec extends AnyFlatSpec {
  import FloatingPointSpec._

  val mpjsons = new MPJsons

  "Serializer" must "keep the JVM text form of finite floating point numbers" in {
    mpjsons.serialize(0.75) mustBe "0.75"
    mpjsons.serialize(1.0e10) mustBe "1.0E10"
    mpjsons.serialize(-2.5f) mustBe "-2.5"
    mpjsons.serialize(Numbers(1.5, 2.5f, 3.5)) mustBe """{"boxed":3.5,"f":2.5,"d":1.5}"""
  }

  "Serializer" must "reject non-finite floating point numbers because JSON cannot represent them" in {
    an[IllegalArgumentException] must be thrownBy mpjsons.serialize(Double.NaN)
    an[IllegalArgumentException] must be thrownBy mpjsons.serialize(Double.PositiveInfinity)
    an[IllegalArgumentException] must be thrownBy mpjsons.serialize(Double.NegativeInfinity)
    an[IllegalArgumentException] must be thrownBy mpjsons.serialize(Float.NaN)
    an[IllegalArgumentException] must be thrownBy mpjsons.serialize(Float.PositiveInfinity)
    an[IllegalArgumentException] must be thrownBy mpjsons.serialize(Numbers(Double.NaN, 1f, 1.0))
    an[IllegalArgumentException] must be thrownBy mpjsons.serialize(Numbers(1.0, 1f, Double.NaN))
  }

  "Serializer" must "name the offending field when a bean contains a non-finite number" in {
    val e = intercept[IllegalArgumentException] { mpjsons.serialize(Numbers(1.0, Float.NaN, 1.0)) }
    e.getMessage must include("NaN")
  }
}
