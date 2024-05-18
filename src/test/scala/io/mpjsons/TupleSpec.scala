package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

class TupleDataObject {
  var tuple1:Tuple1[Int] = _
  var tuple2:(Int, String) = _
  var tuple3:(Int, String, Int) = _
  var tuple4:(Int, String, Int, String) = _
  var tuple5:(String, Int, String, Int, String) = _
  var tuple6:(String, Int, String, Int, String, Int) = _
}

class TupleSpec extends AnyFlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle tuples deserialization with whitespaces" in {

    val json = " {  tuple1 :  [ 3 ], tuple2 :  [ 5 , \"Hello\" ], tuple3 :  [ 5 , \"Hello\", 8 ], tuple4 :  [ 5 , \"Hello\", 8, \"World\" ], tuple5 :  [ \"Hello\", 2, \"World\", 5, \"Hey\" ], tuple6 :  [ \"Hello\", 2, \"World\", 5, \"Hey\", 7 ] } "

    val deserialized = mpjsons.deserialize[TupleDataObject](json)

    deserialized.tuple1 mustBe Tuple1(3)
    deserialized.tuple2 mustBe (5, "Hello")
    deserialized.tuple3 mustBe (5, "Hello", 8)
    deserialized.tuple4 mustBe (5, "Hello", 8, "World")
    deserialized.tuple5 mustBe ("Hello", 2, "World", 5, "Hey")
    deserialized.tuple6 mustBe ("Hello", 2, "World", 5, "Hey", 7)
  }


  "Serializer" must "serialize tuple1" in {

    val toSerialize = Tuple1("Hello")

    val serialized = mpjsons.serialize(toSerialize)

    serialized mustBe "[\"Hello\"]"
  }

  "Serializer" must "serialize tuple2" in {

    val toSerialize = ("Hello", 2)

    val serialized = mpjsons.serialize(toSerialize)

    serialized mustBe "[\"Hello\",2]"
  }

  "Serializer" must "serialize tuple3" in {

    val toSerialize = ("Hello", 2, "World")

    val serialized = mpjsons.serialize(toSerialize)

    serialized mustBe "[\"Hello\",2,\"World\"]"
  }

  "Serializer" must "serialize tuple4" in {

    val toSerialize = ("Hello", 2, "World", 5)

    val serialized = mpjsons.serialize(toSerialize)

    serialized mustBe "[\"Hello\",2,\"World\",5]"
  }

  "Serializer" must "serialize tuple5" in {

    val toSerialize = ("Hello", 2, "World", 5, "Hey")

    val serialized = mpjsons.serialize(toSerialize)

    serialized mustBe "[\"Hello\",2,\"World\",5,\"Hey\"]"
  }

  "Serializer" must "serialize tuple6" in {

    val toSerialize = ("Hello", 2, "World", 5, "Hey", 7)

    val serialized = mpjsons.serialize(toSerialize)

    serialized mustBe "[\"Hello\",2,\"World\",5,\"Hey\",7]"
  }

}
