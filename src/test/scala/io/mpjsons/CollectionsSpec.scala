package io.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

import scala.collection.immutable.Queue

class CollectionsDataObject {
  var stringsList: List[String] = _
  var longsList: List[Long] = _
  var emptyList: List[String] = _
  var emptyArray: Array[Long] = Array()
  var longsSet: Set[Long] = _
  var stringsVector: Vector[String] = _
  var stringsStream: Stream[String] = _
  var stringsQueue: Queue[String] = _
}

class CollectionsSpec extends FlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle collections" in {

    val cdo = new CollectionsDataObject

    cdo.stringsList = List[String]("a", "b", "c")
    cdo.longsList = List[Long](5, 10, 20)
    cdo.emptyList = List[String]()
    cdo.longsSet = Set[Long](1, 11, 111)
    cdo.stringsVector = Vector[String]("x", "Y", "zz")
    cdo.stringsQueue = Queue("An", "Be", "Do")
    cdo.stringsStream = Stream.iterate("A")(_ + "A").take(5)


    val simpleJson = mpjsons.serialize(cdo)
    val dataObject = mpjsons.deserialize[CollectionsDataObject](simpleJson)

    val deserializedObject = dataObject.asInstanceOf[CollectionsDataObject]

    deserializedObject.stringsList mustEqual cdo.stringsList
    deserializedObject.longsList mustEqual cdo.longsList
    deserializedObject.emptyList mustEqual cdo.emptyList
    deserializedObject.emptyArray mustEqual cdo.emptyArray
    deserializedObject.longsSet mustEqual cdo.longsSet
    deserializedObject.stringsVector mustEqual cdo.stringsVector
    deserializedObject.stringsQueue mustEqual cdo.stringsQueue
    deserializedObject.stringsStream mustEqual cdo.stringsStream


  }
}
