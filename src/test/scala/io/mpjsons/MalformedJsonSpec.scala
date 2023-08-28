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

}
