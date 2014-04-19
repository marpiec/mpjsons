package pl.marpiec.mpjsons.immutableseq

import scala.collection.immutable._
import pl.marpiec.mpjsons.MPJson
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

    val objectDeserialized = MPJson.deserialize(json, classOf[TestType])

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
    case class T(f:List[String])
    val objectDeserialized = MPJson.deserializeGeneric(json, classOf[List[String]], classOf[T].getDeclaredField("f"))

    assertEquals(list, objectDeserialized)
  }
}
