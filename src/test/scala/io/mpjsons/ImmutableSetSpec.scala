package io.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

import scala.collection.immutable._

/**
 * @author Marcin Pieciukiewicz
 */

class ImmutableSetType {
  var set: Set[String] = _
  var hashSet: HashSet[String] = _
  var bitSet: BitSet = _
  var listSet: ListSet[String] = _
}


class ImmutableSetSpec extends FlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle immutable sets" in {

    val testObject = new ImmutableSetType
    testObject.set = Set("Abc", "Bcd", "Eca")
    testObject.hashSet = HashSet("Zxy", "Xyz", "ZyX")
    testObject.bitSet = BitSet(12, 15, 3, 7)
    testObject.listSet = ListSet("AAA", "CCC", "BBB")

    val json = mpjsons.serialize(testObject)

    val objectDeserialized = mpjsons.deserialize[ImmutableSetType](json)

    objectDeserialized mustBe an [ImmutableSetType]
    val testObjectDeserialized = objectDeserialized.asInstanceOf[ImmutableSetType]

    testObjectDeserialized.set mustEqual testObject.set
    testObjectDeserialized.hashSet mustEqual testObject.hashSet
//    assertEquals(testObjectDeserialized.sortedSet mustEqual testObject.sortedSet
//    assertEquals(testObjectDeserialized.treeSet mustEqual testObject.treeSet
    testObjectDeserialized.bitSet mustEqual testObject.bitSet
    testObjectDeserialized.listSet mustEqual testObject.listSet


  }
}
