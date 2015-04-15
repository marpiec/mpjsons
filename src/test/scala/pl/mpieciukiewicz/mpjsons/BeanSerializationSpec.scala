package pl.mpieciukiewicz.mpjsons


import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

/**
 * @author Marcin Pieciukiewicz
 */

class InnerObject {
  var intValue: Int = _
  var stringValue: String = _
}

class SimpleDataObjectA {
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

class SimpleDataObjectB {
  var longValue: Long = _
  var stringValue: String = _
  var tuple: (String, String) = _
  var tuplePrimitive: (Int, Long) = _
}

class BeanSerializationSpec extends FlatSpec {

  val sdo = new SimpleDataObjectA
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
  
  sdo.emptyArray = Array[Long]()

  val properJsonSometimesIdentifiersWithoutQuotes = " { " +
    "charValue  : \"M\"  " +
    "longValue  : 1234567891234 , " +
    "intValue : 1111 , " +
    "\"stringValue\" : \"Hello Json \\n\\\" parser\\\\serializer \\\"\" , " +
    "booleanValue : true , " +
    "innerObject : { " +
    "\"intValue\" : -2222 , " +
    "stringValue : \"inner string\" " +
    "} , " +
    "arrayObject : [ \"Be\" , \"or\" , \"not to be\" ] , " +
    "\"arrayPrimitive\":[6,8,10]," +
    "listObject:[\"Hello\",\"json\", \"serializer\"] , " +
    "\"listPrimitive\" : [ 15 , 30 , 1 ]," +
    "emptyArray : [ ] " +
    "} "


  val properJsonNonWhitespaces = "{" +
    "\"charValue\":\"M\"," +
    "\"longValue\":1234567891234," +
    "\"intValue\":1111," +
    "\"stringValue\":\"Hello Json \\n\\\" parser\\\\serializer \\\"\"," +
    "\"booleanValue\":true," +
    "\"innerObject\":{" +
    "\"intValue\":-2222," +
    "\"stringValue\":\"inner string\"" +
    "}," +
    "\"arrayObject\":[\"Be\",\"or\",\"not to be\"]," +
    "\"arrayPrimitive\":[6,8,10]," +
    "\"listObject\":[\"Hello\",\"json\",\"serializer\"]," +
    "\"listPrimitive\":[15,30,1]," +
    "\"emptyArray\":[]" +
    "}"


  "Serializer" must "handle simple deserialization" in {
    val deserialized = MPJson.deserialize[SimpleDataObjectA](properJsonSometimesIdentifiersWithoutQuotes)

    deserialized.longValue mustEqual sdo.longValue
    deserialized.intValue mustEqual sdo.intValue
    deserialized.stringValue mustEqual sdo.stringValue
    deserialized.booleanValue mustEqual sdo.booleanValue

    deserialized.innerObject must not be null
    deserialized.innerObject.intValue mustEqual sdo.innerObject.intValue
    deserialized.innerObject.stringValue mustEqual sdo.innerObject.stringValue

    deserialized.arrayObject must not be null
    deserialized.arrayObject(0) mustEqual sdo.arrayObject(0)
    deserialized.arrayObject(1) mustEqual sdo.arrayObject(1)
    deserialized.arrayObject(2) mustEqual sdo.arrayObject(2)

    deserialized.arrayPrimitive must not be null
    deserialized.arrayPrimitive(0) mustEqual sdo.arrayPrimitive(0)
    deserialized.arrayPrimitive(1) mustEqual sdo.arrayPrimitive(1)
    deserialized.arrayPrimitive(2) mustEqual sdo.arrayPrimitive(2)

    deserialized.listObject must not be null
    deserialized.listObject(0) mustEqual sdo.listObject(0)
    deserialized.listObject(1) mustEqual sdo.listObject(1)
    deserialized.listObject(2) mustEqual sdo.listObject(2)

    deserialized.listPrimitive must not be null
    deserialized.listPrimitive(0) mustEqual sdo.listPrimitive(0)
    deserialized.listPrimitive(1) mustEqual sdo.listPrimitive(1)
    deserialized.listPrimitive(2) mustEqual sdo.listPrimitive(2)

    deserialized.emptyArray must not be null
    deserialized.emptyArray.length mustBe 0
  }




  "Serializer" must "handle simple serialization" in {
    val serialized = MPJson.serialize(sdo)
    serialized mustEqual properJsonNonWhitespaces
  }

  "Serializer" must "handle deserialization with field names in quotes" in {
    val json = "{\"intValue\":10,\"stringValue\":\"Hello\"}"

    val deserialized = MPJson.deserialize[InnerObject](json)

    deserialized.intValue mustEqual 10
    deserialized.stringValue mustEqual "Hello"
  }

  "Serializer" must "handle deserialization with whitespaces" in {
    val json = " {  \"intValue\"  :  10  ,  stringValue  :   \"Hello\"   }   "

    val deserialized = MPJson.deserialize[InnerObject](json)

    deserialized.intValue mustEqual 10
    deserialized.stringValue mustEqual "Hello"
  }

  "Serializer" must "handle serialization and deserialization" in {

    val sdo = new SimpleDataObjectB
    sdo.longValue = 4
    sdo.stringValue = "testString"
    sdo.tuple = ("test", "4")
    sdo.tuplePrimitive = (3, 15)

    val simpleJson = MPJson.serialize(sdo)

    val sdoFromJson = MPJson.deserialize[SimpleDataObjectB](simpleJson)

    sdoFromJson mustBe a [SimpleDataObjectB]
    sdoFromJson.asInstanceOf[SimpleDataObjectB].longValue mustEqual sdo.longValue
    sdoFromJson.asInstanceOf[SimpleDataObjectB].stringValue mustEqual sdo.stringValue
    val (t1: String, t2: String) = sdoFromJson.asInstanceOf[SimpleDataObjectB].tuple
    t1 mustBe "test"
    t2 mustBe "4"
    val (p1: Int, p2: Long) = sdoFromJson.asInstanceOf[SimpleDataObjectB].tuplePrimitive
    p1 mustBe 3
    p2 mustBe 15
  }

}
