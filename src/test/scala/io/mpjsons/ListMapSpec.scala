package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import scala.collection.immutable.ListMap


case class ListMapEntry(name: String)


class ListMapSpec extends AnyFlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle maps" in {


    var map = new ListMap[String, ListMapEntry]

    map += "A" -> ListMapEntry("A")
    map += "E" -> ListMapEntry("E")
    map += "D" -> ListMapEntry("D")
    map += "C" -> ListMapEntry("C")
    map += "B" -> ListMapEntry("B")


    val json = mpjsons.serialize(map)

    json mustEqual """[["A",{"name":"A"}],["E",{"name":"E"}],["D",{"name":"D"}],["C",{"name":"C"}],["B",{"name":"B"}]]"""
  }


  "deserializer" must "preserve order" in {

    val json = """[["A",{"name":"A"}],["E",{"name":"E"}],["D",{"name":"D"}],["C",{"name":"C"}],["B",{"name":"B"}]]"""

    var map = mpjsons.deserialize[ListMap[String, ListMapEntry]](json)

    val entries = map.toList

    entries mustEqual List("A" -> ListMapEntry("A"),
                           "E" -> ListMapEntry("E"),
                           "D" -> ListMapEntry("D"),
                           "C" -> ListMapEntry("C"),
                           "B" -> ListMapEntry("B"))

  }

}