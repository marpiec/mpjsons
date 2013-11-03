package pl.marpiec.mpjsons

import org.testng.annotations.Test
import org.testng.Assert._
import pl.marpiec.mpjsons.annotation.{SecondSubType, FirstSubType}
import scala.collection.immutable.{Stack, Queue}


/**
 * @author Marcin Pieciukiewicz
 */

class SimpleDataObject {
  var longValue: Long = _
  var stringValue: String = _
  var tuple: (String, String) = _
  @FirstSubType(classOf[Int])
  @SecondSubType(classOf[Long])
  var tuplePrimitive: (Int, Long) = _
}

class OptionalDataObject {
  @FirstSubType(classOf[Int])
  var intOption: Option[Int] = _
  @FirstSubType(classOf[Long])
  var smallLongOption: Option[Long] = _
  @FirstSubType(classOf[Long])
  var longOption: Option[Long] = _
  @FirstSubType(classOf[Double])
  var doubleOption: Option[Double] = _
  @FirstSubType(classOf[Boolean])
  var booleanOption: Option[Boolean] = _
  var stringOption: Option[String] = _
  var sdo: Option[SimpleDataObject] = _
}

class CollectionsDataObject {
  var stringsList: List[String] = _
  @FirstSubType(classOf[Long])
  var longsList: List[Long] = _
  var emptyList: List[String] = _
  var emptyArray: Array[Long] = Array()
  @FirstSubType(classOf[Long])
  var longsSet: Set[Long] = _
  var stringsVector: Vector[String] = _
  var stringsStream: Stream[String] = _
  var stringsQueue: Queue[String] = _
  var stringsStack: Stack[String] = _

}

@Test
class JsonSerializerTest {

  def testSimpleObjectSerializationAndDeserialization() {

    val sdo = new SimpleDataObject
    sdo.longValue = 4
    sdo.stringValue = "testString"
    sdo.tuple = ("test", "4")
    sdo.tuplePrimitive = (3, 15)

    val simpleJson = MPJson.serialize(sdo)

    val sdoFromJson = MPJson.deserialize(simpleJson, classOf[SimpleDataObject])

    assertTrue(sdoFromJson.isInstanceOf[SimpleDataObject])
    assertEquals(sdoFromJson.asInstanceOf[SimpleDataObject].longValue, sdo.longValue)
    assertEquals(sdoFromJson.asInstanceOf[SimpleDataObject].stringValue, sdo.stringValue)
    val (t1: String, t2: String) = sdoFromJson.asInstanceOf[SimpleDataObject].tuple
    assertEquals(t1, "test")
    assertEquals(t2, "4")
    val (p1: Int, p2: Long) = sdoFromJson.asInstanceOf[SimpleDataObject].tuplePrimitive
    assertEquals(p1, 3)
    assertEquals(p2, 15)
  }

  def testEmptyOptionSerializationAndDeserialization() {


    val odo = new OptionalDataObject
    odo.intOption = None
    odo.longOption = None
    odo.doubleOption = None
    odo.booleanOption = None
    odo.stringOption = None

    val simpleJson = MPJson.serialize(odo)

    val dataObject = MPJson.deserialize(simpleJson, classOf[OptionalDataObject])

    assertTrue(dataObject.isInstanceOf[OptionalDataObject])

    val odoFromJson = dataObject.asInstanceOf[OptionalDataObject]

    assertTrue(odoFromJson.intOption.isEmpty)
    assertTrue(odoFromJson.longOption.isEmpty)
    assertTrue(odoFromJson.doubleOption.isEmpty)
    assertTrue(odoFromJson.booleanOption.isEmpty)
    assertTrue(odoFromJson.stringOption.isEmpty)
  }


  def testFilledOptionSerializationAndDeserialization() {

    val odo = new OptionalDataObject
    odo.intOption = Option[Int](3)
    odo.smallLongOption = Option[Long](10L)
    odo.longOption = Option[Long](10000000000L) //that is larger than max int
    odo.doubleOption = Option[Double](123.321)
    odo.booleanOption = Option[Boolean](true)
    odo.stringOption = Option[String]("test")

    val sdo = new SimpleDataObject
    sdo.longValue = 4
    sdo.stringValue = "testString"
    odo.sdo = Option[SimpleDataObject](sdo)

    val simpleJson = MPJson.serialize(odo)

    val dataObject = MPJson.deserialize(simpleJson, classOf[OptionalDataObject])

    assertTrue(dataObject.isInstanceOf[OptionalDataObject])

    val odoFromJson = dataObject.asInstanceOf[OptionalDataObject]
    assertTrue(odoFromJson.intOption.isDefined)
    assertTrue(odoFromJson.smallLongOption.isDefined)
    assertTrue(odoFromJson.longOption.isDefined)
    assertTrue(odoFromJson.doubleOption.isDefined)
    assertTrue(odoFromJson.booleanOption.isDefined)
    assertTrue(odoFromJson.stringOption.isDefined)
    assertTrue(odoFromJson.sdo.isDefined)
    assertEquals(dataObject.asInstanceOf[OptionalDataObject].intOption.get, odo.intOption.get)
    assertEquals(dataObject.asInstanceOf[OptionalDataObject].smallLongOption.get, odo.smallLongOption.get)
    assertEquals(dataObject.asInstanceOf[OptionalDataObject].longOption.get, odo.longOption.get)
    assertEquals(dataObject.asInstanceOf[OptionalDataObject].doubleOption.get, odo.doubleOption.get)
    assertEquals(dataObject.asInstanceOf[OptionalDataObject].booleanOption.get, odo.booleanOption.get)
    assertEquals(dataObject.asInstanceOf[OptionalDataObject].stringOption.get, odo.stringOption.get)
    assertEquals(dataObject.asInstanceOf[OptionalDataObject].sdo.get.longValue, odo.sdo.get.longValue)
    assertEquals(dataObject.asInstanceOf[OptionalDataObject].sdo.get.stringValue, odo.sdo.get.stringValue)

  }


  def testCollectionsSerialization() {

    val cdo = new CollectionsDataObject

    cdo.stringsList = List[String]("a", "b", "c")
    cdo.longsList = List[Long](5, 10, 20)
    cdo.emptyList = List[String]()
    cdo.longsSet = Set[Long](1, 11, 111)
    cdo.stringsVector = Vector[String]("x", "Y", "zz")
    cdo.stringsQueue = Queue("An", "Be", "Do")
    cdo.stringsStack = Stack("F", "G", "h", "o")
    cdo.stringsStream = Stream.iterate("A")(_ + "A").take(5)


    val simpleJson = MPJson.serialize(cdo)
    val dataObject = MPJson.deserialize(simpleJson, classOf[CollectionsDataObject])

    val deserializedObject = dataObject.asInstanceOf[CollectionsDataObject]
    assertEquals(deserializedObject.stringsList, cdo.stringsList)

    assertEquals(deserializedObject.longsList, cdo.longsList)

    assertEquals(deserializedObject.emptyList, cdo.emptyList)
    assertEquals(deserializedObject.emptyArray, cdo.emptyArray)

    assertEquals(deserializedObject.longsSet, cdo.longsSet)

    assertEquals(deserializedObject.stringsVector, cdo.stringsVector)

    assertEquals(deserializedObject.stringsQueue, cdo.stringsQueue)
    assertEquals(deserializedObject.stringsStack, cdo.stringsStack)
    assertEquals(deserializedObject.stringsStream, cdo.stringsStream)


  }

}
