package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

object SkipFieldsSpec {

  case class TestObject(a: Boolean, b: Int)

}

class SkipFieldsSpec extends FlatSpec {

  val value = """{"a": true, "c": "Hello", "d": [1,2,3,4], "e":{"a":false, "b":[{}], "c":"a"}, "f": false, "b": 3}"""

  "Deserializer" must "handle additional fields if ignore mode" in {
    val mpjsons = new MPJsons(true)
    val deserialized = mpjsons.deserialize[SkipFieldsSpec.TestObject](value)
    deserialized.a mustEqual true
    deserialized.b mustEqual 3

  }


  "Deserializer" must "throw exception on additional fields in default mode" in {
    val mpjsons = new MPJsons()
    try {
      mpjsons.deserialize[SkipFieldsSpec.TestObject](value)
      throw new IllegalStateException("Expression should throw an exception")
    } catch {
      case e: JsonInnerException => () // ok
    }
  }

}
