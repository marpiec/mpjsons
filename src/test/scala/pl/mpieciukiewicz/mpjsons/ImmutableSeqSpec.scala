package pl.mpieciukiewicz.mpjsons

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
  var stack: Stack[String] = _
}


class ImmutableSeqSpec extends FlatSpec {


  "Serializer" must "handle immutable sequences" in {

    val testObject = new ImmutableSeqType
    testObject.vector = Vector("Abc", "Bcd", "Eca")
    testObject.list = List("Zxy", "Xyz", "ZyX")
    testObject.stream = Stream("kkk","qwe", "dfs")
    testObject.queue = Queue("AAA", "CCC", "BBB")
    testObject.stack = Stack("MMM", "efce", "abc")

    val json = MPJson.serialize(testObject)

    val objectDeserialized = MPJson.deserialize[ImmutableSeqType](json)

    objectDeserialized mustBe an [ImmutableSeqType]
    val testObjectDeserialized = objectDeserialized.asInstanceOf[ImmutableSeqType]

    testObjectDeserialized.vector mustEqual testObject.vector
    testObjectDeserialized.list mustEqual testObject.list
    testObjectDeserialized.stream mustEqual testObject.stream
    testObjectDeserialized.queue mustEqual testObject.queue
    testObjectDeserialized.stack mustEqual testObject.stack


  }

  "Serializer" must "handle collections without wrappers" in {
    val list = List("Zxy", "Xyz", "ZyX")

    val json = MPJson.serialize(list)
    val objectDeserialized = MPJson.deserializeGeneric[List[String]](json)

    list mustEqual objectDeserialized
  }
}
