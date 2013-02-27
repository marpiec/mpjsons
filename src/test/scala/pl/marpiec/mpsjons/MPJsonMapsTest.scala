package pl.marpiec.mpjsons

/**
 * @author Marcin Pieciukiewicz
 */

import org.testng.Assert._
import org.testng.annotations.Test
import pl.marpiec.mpjsons.annotation.{SecondSubType, FirstSubType}

// this tests also non default constructor and final fields
class MapElement(var intValue: Int, val stringValue: String)

class SimpleMapsObject {
  
  var simpleMap:Map[String, String] = _
  @FirstSubType(classOf[Int]) @SecondSubType(classOf[Long])
  var primitiveMap:Map[Int, Long] = _
  var objectMap:Map[MapElement, MapElement] = _
  
}

@Test
class MPJsonMapsTest {

  def testMapSerialization() {

    var smo = new SimpleMapsObject

    smo.simpleMap = Map()
    smo.simpleMap += "a" -> "Ala"
    smo.simpleMap += "k" -> "Kot"

    smo.primitiveMap = Map()
    smo.primitiveMap += 1 -> 1224
    smo.primitiveMap += 5 -> 5324

    smo.objectMap = Map()
    
    smo.objectMap += new MapElement(1, "one") -> new MapElement(100, "one hundred")
    smo.objectMap += new MapElement(5, "five") -> new MapElement(500, "five hundred")

    val serialized = MPJson.serialize(smo)

    assertEquals(serialized, "{simpleMap:[{k:\"a\",v:\"Ala\"},{k:\"k\",v:\"Kot\"}]," +
                              "primitiveMap:[{k:1,v:1224},{k:5,v:5324}]," +
                              "objectMap:[" +
                                "{k:{intValue:1,stringValue:\"one\"},v:{intValue:100,stringValue:\"one hundred\"}}," +
                                "{k:{intValue:5,stringValue:\"five\"},v:{intValue:500,stringValue:\"five hundred\"}}" +
                              "]}")


    val serializedWithWhitespacesQuotesAndInvertedKV = "  {  simpleMap  :  [  {  \"k\" : \"a\" , \"v\" : \"Ala\" } , {  k : \"k\" , v : \"Kot\" } ] , " +
      "primitiveMap : [ { k : 1 , v : 1224 } , { v : 5324 , \"k\" : 5} ] , " +
      "objectMap : [ " +
      "{ k : { intValue : 1 , stringValue : \"one\" } , v  : { intValue : 100 , stringValue : \"one hundred\" } } , " +
      "{k:{intValue:5,stringValue:\"five\"},v:{intValue:500,stringValue:\"five hundred\"}} " +
      "] } "

    val smoDeserialized:SimpleMapsObject = MPJson.deserialize(serializedWithWhitespacesQuotesAndInvertedKV, classOf[SimpleMapsObject]).asInstanceOf[SimpleMapsObject]

    assertNotNull(smoDeserialized)

    assertEquals(smoDeserialized.simpleMap, smo.simpleMap)
    assertEquals(smoDeserialized.primitiveMap, smo.primitiveMap)
    assertEquals(smoDeserialized.objectMap.size, smo.objectMap.size)
  }

}
