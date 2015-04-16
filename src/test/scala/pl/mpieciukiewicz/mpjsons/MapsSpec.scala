package pl.mpieciukiewicz.mpjsons

/**
 * @author Marcin Pieciukiewicz
 */

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

// this tests also non default constructor and final fields
class MapElement(var intValue: Int, val stringValue: String)

class SimpleMapsObject {

  var emptyMap:Map[Int, Long] = Map()
  var simpleMap:Map[String, String] = _
  var primitiveMap:Map[Int, Long] = _
  var objectMap:Map[MapElement, MapElement] = _
  
}

class MapsSpec extends FlatSpec {

  "Serializer" must "handle maps" in {

    val smo = new SimpleMapsObject

    smo.simpleMap = Map("a" -> "Ala", "k" -> "Kot")
    smo.primitiveMap = Map(1 -> 1224, 5 -> 5324)

    smo.objectMap = Map(
      new MapElement(1, "one") -> new MapElement(100, "one hundred"),
      new MapElement(5, "five") -> new MapElement(500, "five hundred"))

    val serialized = MPJsonS.serialize(smo)

    serialized mustBe
      """{"emptyMap":[],
        |"simpleMap":[["a","Ala"],["k","Kot"]],
        |"primitiveMap":[[1,1224],[5,5324]],
        |"objectMap":[[{"intValue":1,"stringValue":"one"},
        |{"intValue":100,"stringValue":"one hundred"}],
        |[{"intValue":5,"stringValue":"five"},
        |{"intValue":500,"stringValue":"five hundred"}]]}""".stripMargin.lines.mkString("")


    val serializedWithWhitespacesQuotes =
      """  {  "emptyMap"  :   [   ]  ,
        |  "simpleMap"  :  [  ["a" , "Ala" ] , [  "k" , "Kot"  ] ]  ,
        |   "primitiveMap" : [  [1 , 1224]  ,  [ 5  , 5324  ] ],
        |    "objectMap" : [ [  { "intValue" : 1 , "stringValue" : "one" } ,
        |     { "intValue" : 100 , "stringValue" : "one hundred" } ] ,
        |      [ {"intValue":5,"stringValue":"five"},
        |      {"intValue":500,"stringValue":"five hundred"} ] ] } """.stripMargin.lines.mkString("")

    val smoDeserialized:SimpleMapsObject = MPJsonS.deserialize[SimpleMapsObject](serializedWithWhitespacesQuotes)

    smoDeserialized must not be (null)

    smoDeserialized.simpleMap mustEqual smo.simpleMap
    smoDeserialized.primitiveMap mustEqual smo.primitiveMap
    smoDeserialized.objectMap.size mustEqual smo.objectMap.size
  }

}
