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

  "Deserializer" must "fail gracefully on empty input for a primitive" in {
    intercept[JsonInnerException] {
      mpjsons.deserialize[Int]("")
    }
  }

}
