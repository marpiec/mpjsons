package pl.marpiec.mpjsons.immutable.map

import scala.collection.immutable._
import pl.marpiec.mpjsons.MPJson
import org.testng.Assert._
import org.testng.annotations.Test

/**
 * @author Marcin Pieciukiewicz
 */

class TestType {
  var map: Map[String, String] = _
  var hashMap: HashMap[String, String] = _
//  var sortedMap: SortedMap[String, String] = _
//  var treeMap: TreeMap[String, String] = _
  var listMap: ListMap[String, String] = _
}


@Test
class ImmutableMapTest {

  def immutableMapTest() {


    val testObject = new TestType
    testObject.map = Map("a" -> "abc", "b" -> "bca", "c" -> "cab")
    testObject.hashMap = HashMap("a" -> "abc", "b" -> "bca", "c" -> "cab")
//    testObject.sortedMap = SortedMap("a" -> "abc", "b" -> "bca", "c" -> "cab")
//    testObject.treeMap = TreeMap("a" -> "abc", "b" -> "bca", "c" -> "cab")
    testObject.listMap = ListMap("a" -> "abc", "b" -> "bca", "c" -> "cab")


    val json = MPJson.serialize(testObject)

    val objectDeserialized = MPJson.deserialize(json, classOf[TestType])

    assertTrue(objectDeserialized.isInstanceOf[TestType])
    val testObjectDeserialized = objectDeserialized.asInstanceOf[TestType]

    assertEquals(testObjectDeserialized.map, testObject.map)
    assertEquals(testObjectDeserialized.hashMap, testObject.hashMap)
//    assertEquals(testObjectDeserialized.sortedMap, testObject.sortedMap)
//    assertEquals(testObjectDeserialized.treeMap, testObject.treeMap)
    assertEquals(testObjectDeserialized.listMap, testObject.listMap)


  }
}
