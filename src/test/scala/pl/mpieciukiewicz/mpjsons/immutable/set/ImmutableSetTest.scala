package pl.mpieciukiewicz.mpjsons.immutable.set

import scala.collection.immutable._
import pl.mpieciukiewicz.mpjsons.MPJson
import org.testng.Assert._
import org.testng.annotations.Test

/**
 * @author Marcin Pieciukiewicz
 */

class TestType {
  var set: Set[String] = _
  var hashSet: HashSet[String] = _
  var sortedSet: SortedSet[String] = _
  var treeSet: TreeSet[String] = _
  var bitSet: BitSet = _
  var listSet: ListSet[String] = _
}


@Test
class ImmutableSetTest {

  def immutableSetTest() {


    val testObject = new TestType
    testObject.set = Set("Abc", "Bcd", "Eca")
    testObject.hashSet = HashSet("Zxy", "Xyz", "ZyX")
//    testObject.sortedSet = SortedSet("MNP", "mn", "pmn", "aaa")
//    testObject.treeSet = TreeSet("C", "D", "A")
    testObject.bitSet = BitSet(12, 15, 3, 7)
    testObject.listSet = ListSet("AAA", "CCC", "BBB")

    val json = MPJson.serialize(testObject)

    val objectDeserialized = MPJson.deserialize[TestType](json)

    assertTrue(objectDeserialized.isInstanceOf[TestType])
    val testObjectDeserialized = objectDeserialized.asInstanceOf[TestType]

    assertEquals(testObjectDeserialized.set, testObject.set)
    assertEquals(testObjectDeserialized.hashSet, testObject.hashSet)
//    assertEquals(testObjectDeserialized.sortedSet, testObject.sortedSet)
//    assertEquals(testObjectDeserialized.treeSet, testObject.treeSet)
    assertEquals(testObjectDeserialized.bitSet, testObject.bitSet)
    assertEquals(testObjectDeserialized.listSet, testObject.listSet)


  }
}
