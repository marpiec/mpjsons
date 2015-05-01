package io.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

case class RecursiveType(subtypes: List[RecursiveType])


class InfiniteDepthSpec extends FlatSpec {
  val mpjsons = new MPJsons


  "Serializer" must "handle possibly infinite depth of object" in {
    val testObject = RecursiveType(List(RecursiveType(List(RecursiveType(List()))), RecursiveType(List())))

    val json = mpjsons.serialize(testObject)

    val objectDeserialized = mpjsons.deserialize[RecursiveType](json)

    objectDeserialized mustBe an [RecursiveType]
    val testObjectDeserialized = objectDeserialized.asInstanceOf[RecursiveType]

    testObjectDeserialized mustBe testObject

  }

}
