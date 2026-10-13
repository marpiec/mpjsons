package io.mpjsons

import io.mpjsons.impl.JsonInnerException

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._


class MalformedJsonSpec extends AnyFlatSpec {

  case class Sample(a: Int, b: String)

  val mpjsons = new MPJsons

  "Serializer" must "fail gracefully on malformed jsons" in {

    intercept[JsonInnerException] {
      val deserialized: Sample = mpjsons.deserialize[Sample]("""{"a":12,"b":"c" """)

    }
  }

  "Deserializer" must "fail gracefully on empty input" in {
    val e = intercept[JsonInnerException] {
      mpjsons.deserialize[Sample]("")
    }
    e.getMessage must include("Sample")
  }

  "Deserializer" must "fail gracefully on whitespace-only input" in {
    intercept[JsonInnerException] {
      mpjsons.deserialize[Sample]("   ")
    }
  }

  "Deserializer" must "fail gracefully on empty input for a static deserializer" in {
    val deserializer = mpjsons.buildStaticDeserializer[Sample]
    intercept[JsonInnerException] {
      deserializer.deserialize("")
    }
  }

  "Deserializer" must "keep error messages small instead of copying the whole input" in {
    val longNumber = """{"a":""" + "1" * 10000 + ""","b":"c"}"""
    val e1 = intercept[JsonInnerException] { mpjsons.deserialize[Sample](longNumber) }
    e1.getMessage.length must be < 1000
    e1.getMessage must include("10000 chars")

    val longKey = "{\"" + "k" * 10000 + "\":1}"
    val e2 = intercept[JsonInnerException] { mpjsons.deserialize[Sample](longKey) }
    e2.getMessage.length must be < 1000
    e2.getMessage must include("No field [kkk")

    val padded = """{"a":12,"b":"c" ,""" + " " * 10000 + """ "zzz":1}"""
    val e3 = intercept[JsonInnerException] { mpjsons.deserialize[Sample](padded) }
    e3.getMessage.length must be < 1000
    e3.getMessage must not include ("Json:")
  }

  "Deserializer" must "point the caret at the failing character" in {
    val e = intercept[JsonInnerException] { mpjsons.deserialize[Sample]("""{"a":"oops","b":"c"}""") }
    val lines = e.getMessage.linesIterator.toList
    val window = lines(lines.size - 2)
    val caret = lines.last
    e.getMessage must include("At position 5 of 20")
    window.charAt(caret.indexOf('^')) mustBe '"'

    val nested = "{\n  \"a\": 1,\n  \"b\": 2\n}"
    val e2 = intercept[JsonInnerException] { mpjsons.deserialize[Sample](nested) }
    val lines2 = e2.getMessage.linesIterator.toList
    lines2(lines2.size - 2).charAt(lines2.last.indexOf('^')) mustBe '2'
  }

  "Deserializer" must "fail gracefully on empty input for a primitive" in {
    intercept[JsonInnerException] {
      mpjsons.deserialize[Int]("")
    }
  }

}
