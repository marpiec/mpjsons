package io.mpjsons

import io.mpjsons.PostDeserializationTransformSpec.Sample
import org.scalatest.GivenWhenThen
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object PostDeserializationTransformSpec {
  case class Sample(a: String, b: String)
}

class PostDeserializationTransformSpec extends AnyFlatSpec with GivenWhenThen {


  "Deserializer" must "be able to post process object" in {

    Given("Simple Object")

    val mpjsons = new MPJsons
    mpjsons.postDeserializationTransform[Sample] {(sample: Sample) =>
      if(sample.b == null) {
        sample.copy(b = "")
      } else {
        sample
      }
    }

    val deserialized = mpjsons.deserialize[Sample]("""{a: "Hello"}""")

    deserialized mustBe Sample("Hello", "")
  }

}
