package pl.mpieciukiewicz.mpjsons

import org.scalatest.{GivenWhenThen, MustMatchers, FlatSpec}
import scala.reflect.runtime.universe._
object PerformanceSpec {
  class SimpleDataObject {
    var charValue: Char = _
    var longValue: Long = _
    var intValue: Int = _
    var stringValue: String = _
    var booleanValue: Boolean = _
    var innerObject: InnerObject = _
    var arrayObject: Array[String] = _
    var arrayPrimitive: Array[Long] = _
    var listObject: List[String] = _
    var listPrimitive: List[Int] = _
    var emptyArray: Array[Long] = _
  }
}

class PerformanceSpec extends FlatSpec with MustMatchers with GivenWhenThen {


  "Serializer" must "perform well" in {


    Given("Simple Object")

    val mpjsons = new MPJsons

    val sdo = new PerformanceSpec.SimpleDataObject
    sdo.charValue = 'M'
    sdo.longValue = 1234567891234L
    sdo.intValue = 1111
    sdo.stringValue = "Hello Json \n\" parser\\serializer \"" //Hello Json " parser\deserializer "
    sdo.booleanValue = true

    sdo.innerObject = new InnerObject
    sdo.innerObject.intValue = -2222
    sdo.innerObject.stringValue = "inner string"

    sdo.arrayObject = Array[String]("Be", "or", "not to be")
    sdo.arrayPrimitive = Array[Long](6, 8, 10)

    sdo.listObject = List[String]("Hello", "json", "serializer")
    sdo.listPrimitive = List[Int](15, 30, 1)


    When("Trying to serialize and deserialize multiple times")

    implicit val tt = typeTag[PerformanceSpec.SimpleDataObject]

    for(i <- 0 to 1000) {
      sdo.intValue += 1
      val serialized = mpjsons.serialize(sdo)
      val deserialized = mpjsons.deserialize(serialized)
      sdo.intValue mustBe sdo.intValue
    }

    val start = System.currentTimeMillis()

    for(i <- 0 to 5000) {
      sdo.intValue += 1
      val serialized = mpjsons.serialize(sdo)
      val deserialized = mpjsons.deserialize(serialized)
      sdo.intValue mustBe sdo.intValue
    }

    val end = System.currentTimeMillis()

    Then("We have acceptable performance")

    (end - start) must be < 500L

  }
}
