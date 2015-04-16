package pl.mpieciukiewicz.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

import scala.collection.immutable._


class ImmutableMapType {
  var map: Map[String, String] = _
  var hashMap: HashMap[String, String] = _
//  var sortedMap: SortedMap[String, String] = _
//  var treeMap: TreeMap[String, String] = _
  var listMap: ListMap[String, String] = _
}


class ImmutableMapSpec extends FlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle maps" in {

    val testObject = new ImmutableMapType
    testObject.map = Map("a" -> "abc", "b" -> "bca", "c" -> "cab")
    testObject.hashMap = HashMap("a" -> "abc", "b" -> "bca", "c" -> "cab")
    //    testObject.sortedMap = SortedMap("a" -> "abc", "b" -> "bca", "c" -> "cab")
    //    testObject.treeMap = TreeMap("a" -> "abc", "b" -> "bca", "c" -> "cab")
    testObject.listMap = ListMap("a" -> "abc", "b" -> "bca", "c" -> "cab")


    val json = mpjsons.serialize(testObject)

    val objectDeserialized = mpjsons.deserialize[ImmutableMapType](json)

    objectDeserialized mustBe a [ImmutableMapType]

    val testObjectDeserialized = objectDeserialized.asInstanceOf[ImmutableMapType]

    testObjectDeserialized.map must equal(testObject.map)
    testObjectDeserialized.hashMap must equal(testObject.hashMap)
    //    testObjectDeserialized.sortedMap must equal(testObject.sortedMap)
    //    testObjectDeserialized.treeMap must equal(testObject.treeMap)
    testObjectDeserialized.listMap must equal(testObject.listMap)
  }

}
