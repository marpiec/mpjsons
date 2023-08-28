package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

class TupleDataObject {
  var tuple:(Int, String) = _
}

class TupleSpec extends AnyFlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle tuples deserialization with whitespaces" in {

    val json = " {  tuple :  [ 5 , \"Hello\" ]  }   "

    val deserialized = mpjsons.deserialize[TupleDataObject](json)

    deserialized.tuple mustBe (5, "Hello")
  }

}
