package io.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

import scala.collection.immutable._

/**
 * @author Marcin Pieciukiewicz
 */

class ImmutableSeqType {
  var vector: Vector[String] = _
//  var numericRange: NumericRange[String] = _
//  var range: Range = _
  var list: List[String] = _
  var stream: Stream[String] = _
  var queue: Queue[String] = _
}


class ImmutableSeqSpec extends FlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle immutable sequences" in {

    val testObject = new ImmutableSeqType
    testObject.vector = Vector("Abc", "Bcd", "Eca")
    testObject.list = List("Zxy", "Xyz", "ZyX")
    testObject.stream = Stream("kkk","qwe", "dfs")
    testObject.queue = Queue("AAA", "CCC", "BBB")

    val json = mpjsons.serialize(testObject)

    val objectDeserialized = mpjsons.deserialize[ImmutableSeqType](json)

    objectDeserialized mustBe an [ImmutableSeqType]
    val testObjectDeserialized = objectDeserialized.asInstanceOf[ImmutableSeqType]

    testObjectDeserialized.vector mustEqual testObject.vector
    testObjectDeserialized.list mustEqual testObject.list
    testObjectDeserialized.stream mustEqual testObject.stream
    testObjectDeserialized.queue mustEqual testObject.queue


  }

  "Serializer" must "handle collections without wrappers" in {
    val list = List("Zxy", "Xyz", "ZyX")

    val json = mpjsons.serialize(list)
    val objectDeserialized = mpjsons.deserialize[List[String]](json)

    list mustEqual objectDeserialized
  }
}
