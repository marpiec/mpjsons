package pl.mpieciukiewicz.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

class TupleDataObject {
  var tuple:(Int, String) = _
}

class TupleSpec extends FlatSpec {

  "Serializer" must "handle tuples deserialization with whitespaces" in {

    val json = " {  tuple :  [ 5 , \"Hello\" ]  }   "

    val deserialized = MPJson.deserialize[TupleDataObject](json)

    deserialized.tuple mustBe (5, "Hello")
  }

}
