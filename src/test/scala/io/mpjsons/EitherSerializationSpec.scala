package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

class EitherDataObject {
  var intInt: Either[Int, Int] = _
  var booleanString: Either[Boolean, String] = _
}

class EitherSerializationSpec extends AnyFlatSpec {
  val mpjsons = new MPJsons

  "Serializer" must "handle Either" in {

    val edo = new EitherDataObject
    edo.booleanString = Left(false)
    edo.intInt = Right(2)

    var simpleJson = mpjsons.serialize(edo)
    var deserializedObject = mpjsons.deserialize[EitherDataObject](simpleJson)

    deserializedObject.booleanString mustBe Left(false)
    deserializedObject.intInt mustBe Right(2)


    edo.booleanString = Right("hello")
    edo.intInt = Left(3)

    simpleJson = mpjsons.serialize(edo)
    deserializedObject = mpjsons.deserialize[EitherDataObject](simpleJson)

    deserializedObject.booleanString mustBe Right("hello")
    deserializedObject.intInt mustBe Left(3)

  }
}
