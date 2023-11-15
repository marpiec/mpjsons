package io.mpjsons

/**
 * @author Marcin Pieciukiewicz
 */

import io.mpjsons.impl.{DeserializerFactory, SerializerFactory, StringIterator}
import io.mpjsons.impl.deserializer.SimpleMapDeserializer
import io.mpjsons.impl.serializer.SimpleMapSerializer
import io.mpjsons.impl.util.Context
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

case class SimpleMapElement(intValue: Int, stringValue: String)



class SimpleMapsSpec extends AnyFlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle maps" in {

    val smo = new SimpleMapsObject

    val testMapA: Map[String, String] = Map("a" -> "Ala", "k" -> "Kot")
    val testMapB: Map[String, Double] = Map("a" -> 123.0, "k" -> 423.0)
    val testMapC: Map[String, SimpleMapElement] = Map("a" -> new SimpleMapElement(100, "one hundred"), "k" -> new SimpleMapElement(500, "five hundred"))


   val mapASerializer = new SimpleMapSerializer[String](new SerializerFactory, Context(List.empty, Map.empty))
   val mapBSerializer = new SimpleMapSerializer[Double](new SerializerFactory, Context(List.empty, Map.empty))
   val mapCSerializer = new SimpleMapSerializer[SimpleMapElement](new SerializerFactory, Context(List.empty, Map.empty))

    val builderA = new StringBuilder
    mapASerializer.serialize(testMapA, builderA)
    val mapASerialized = builderA.toString

    val builderB = new StringBuilder
    mapBSerializer.serialize(testMapB, builderB)
    val mapBSerialized = builderB.toString

    val builderC = new StringBuilder
    mapCSerializer.serialize(testMapC, builderC)
    val mapCSerialized = builderC.toString


    val mapADeserializer = new SimpleMapDeserializer[String](new DeserializerFactory(true), Context(List.empty, Map.empty))
    val mapBDeserializer = new SimpleMapDeserializer[Double](new DeserializerFactory(true), Context(List.empty, Map.empty))
    val mapCDeserializer = new SimpleMapDeserializer[SimpleMapElement](new DeserializerFactory(true), Context(List.empty, Map.empty))

    mapADeserializer.deserialize(new StringIterator(mapASerialized)) mustEqual testMapA
    mapBDeserializer.deserialize(new StringIterator(mapBSerialized)) mustEqual testMapB
    mapCDeserializer.deserialize(new StringIterator(mapCSerialized)) mustEqual testMapC

    val serializedA =
      """ {   "a"  :  "Ala"  ,
        |    "k"   :   "Kot"    } """.stripMargin.lines.toArray.mkString("")

    val serializedB =
      """ {   "a"  :  123  ,    "k"   :   423.0    } """.stripMargin.lines.toArray.mkString("")

    val serializedC =
      """ {   "a"  :  {"intValue": 100, stringValue: "one hundred"   }  ,    "k"   :   {"intValue": 500, stringValue: "five hundred"   }   } """.stripMargin.lines.toArray.mkString("")


    mapADeserializer.deserialize(new StringIterator(serializedA)) mustEqual testMapA
    mapBDeserializer.deserialize(new StringIterator(serializedB)) mustEqual testMapB
    mapCDeserializer.deserialize(new StringIterator(serializedC)) mustEqual testMapC




    //    smoDeserialized = mpjsons.deserialize[SimpleMapsObject](serializedWithWhitespacesQuotes)
//
//    smoDeserialized must not be (null)
//
//    smoDeserialized.simpleMap mustEqual smo.simpleMap
//    smoDeserialized.primitiveMap mustEqual smo.primitiveMap
//    smoDeserialized.objectMap.size mustEqual smo.objectMap.size
  }

}
