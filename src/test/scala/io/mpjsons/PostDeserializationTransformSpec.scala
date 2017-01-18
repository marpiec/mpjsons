package io.mpjsons

import io.mpjsons.PostDeserializationTransformSpec.Sample
import org.scalatest.{FlatSpec, GivenWhenThen, MustMatchers}

object PostDeserializationTransformSpec {
  case class Sample(a: String, b: String)
}

class PostDeserializationTransformSpec extends FlatSpec with MustMatchers with GivenWhenThen {


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
