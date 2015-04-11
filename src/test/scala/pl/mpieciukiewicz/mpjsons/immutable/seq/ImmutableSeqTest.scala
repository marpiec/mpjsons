package pl.mpieciukiewicz.mpjsons.immutable.seq

import scala.collection.immutable._
import pl.mpieciukiewicz.mpjsons.MPJson
import org.testng.Assert._
import org.testng.annotations.Test

/**
 * @author Marcin Pieciukiewicz
 */

class TestType {
  var vector: Vector[String] = _
//  var numericRange: NumericRange[String] = _
//  var range: Range = _
  var list: List[String] = _
  var stream: Stream[String] = _
  var queue: Queue[String] = _
  var stack: Stack[String] = _
}


@Test
class ImmutableSeqTest {

  def immutableSeqTest() {


    val testObject = new TestType
    testObject.vector = Vector("Abc", "Bcd", "Eca")
    testObject.list = List("Zxy", "Xyz", "ZyX")
    testObject.stream = Stream("kkk","qwe", "dfs")
    testObject.queue = Queue("AAA", "CCC", "BBB")
    testObject.stack = Stack("MMM", "efce", "abc")

    val json = MPJson.serialize(testObject)

    val objectDeserialized = MPJson.deserialize[TestType](json)

    assertTrue(objectDeserialized.isInstanceOf[TestType])
    val testObjectDeserialized = objectDeserialized.asInstanceOf[TestType]

    assertEquals(testObjectDeserialized.vector, testObject.vector)
    assertEquals(testObjectDeserialized.list, testObject.list)
    assertEquals(testObjectDeserialized.stream, testObject.stream)
    assertEquals(testObjectDeserialized.queue, testObject.queue)
    assertEquals(testObjectDeserialized.stack, testObject.stack)


  }


  def collectionWIthoutWrapperTest() {
    val list = List("Zxy", "Xyz", "ZyX")

    val json = MPJson.serialize(list)
    val objectDeserialized = MPJson.deserializeGeneric[List[String]](json)

    assertEquals(list, objectDeserialized)
  }
}
