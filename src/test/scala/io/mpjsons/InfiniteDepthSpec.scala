package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

case class RecursiveType(subtypes: List[RecursiveType])


class InfiniteDepthSpec extends AnyFlatSpec {
  val mpjsons = new MPJsons


  "Serializer" must "handle possibly infinite depth of object" in {
    val testObject = RecursiveType(List(RecursiveType(List(RecursiveType(List.empty))), RecursiveType(List.empty)))

    val json = mpjsons.serialize(testObject)

    val objectDeserialized = mpjsons.deserialize[RecursiveType](json)

    objectDeserialized mustBe an [RecursiveType]
    val testObjectDeserialized = objectDeserialized.asInstanceOf[RecursiveType]

    testObjectDeserialized mustBe testObject

  }

}
