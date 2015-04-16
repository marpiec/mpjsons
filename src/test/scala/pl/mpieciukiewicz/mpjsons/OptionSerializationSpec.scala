package pl.mpieciukiewicz.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

/**
 * @author Marcin Pieciukiewicz
 */



class OptionalDataObject {
  var intOption: Option[Int] = _
  var smallLongOption: Option[Long] = _
  var longOption: Option[Long] = _
  var doubleOption: Option[Double] = _
  var booleanOption: Option[Boolean] = _
  var stringOption: Option[String] = _
  var sdo: Option[SimpleDataObjectB] = _
}



class OptionSerializationSpec extends FlatSpec {



  "Serializer" must "handle empty Options" in {


    val odo = new OptionalDataObject
    odo.intOption = None
    odo.longOption = None
    odo.doubleOption = None
    odo.booleanOption = None
    odo.stringOption = None

    val simpleJson = MPJson.serialize(odo)

    val dataObject = MPJson.deserialize[OptionalDataObject](simpleJson)

    dataObject mustBe a [OptionalDataObject]

    val odoFromJson = dataObject.asInstanceOf[OptionalDataObject]

    odoFromJson.intOption mustBe 'empty
    odoFromJson.longOption mustBe 'empty
    odoFromJson.doubleOption mustBe 'empty
    odoFromJson.booleanOption mustBe 'empty
    odoFromJson.stringOption mustBe 'empty
  }



  "Serializer" must "handle filled Options" in {

    val odo = new OptionalDataObject
    odo.intOption = Option[Int](3)
    odo.smallLongOption = Option[Long](10L)
    odo.longOption = Option[Long](10000000000L) //that is larger than max int
    odo.doubleOption = Option[Double](123.321)
    odo.booleanOption = Option[Boolean](true)
    odo.stringOption = Option[String]("test")

    val sdo = new SimpleDataObjectB
    sdo.longValue = 4
    sdo.stringValue = "testString"
    odo.sdo = Option[SimpleDataObjectB](sdo)

    val simpleJson = MPJson.serialize(odo)

    val dataObject = MPJson.deserialize[OptionalDataObject](simpleJson)

    dataObject mustBe an [OptionalDataObject]

    val optionalDataObject = dataObject.asInstanceOf[OptionalDataObject]
    val odoFromJson = optionalDataObject
    odoFromJson.intOption mustBe 'defined
    odoFromJson.smallLongOption mustBe 'defined
    odoFromJson.longOption mustBe 'defined
    odoFromJson.doubleOption mustBe 'defined
    odoFromJson.booleanOption mustBe 'defined
    odoFromJson.stringOption mustBe 'defined
    odoFromJson.sdo mustBe 'defined
    optionalDataObject.intOption.get mustBe odo.intOption.get
    optionalDataObject.smallLongOption.get mustBe odo.smallLongOption.get
    optionalDataObject.longOption.get mustBe odo.longOption.get
    optionalDataObject.doubleOption.get mustBe odo.doubleOption.get
    optionalDataObject.booleanOption.get mustBe odo.booleanOption.get
    optionalDataObject.stringOption.get mustBe odo.stringOption.get
    optionalDataObject.sdo.get.longValue mustBe odo.sdo.get.longValue
    optionalDataObject.sdo.get.stringValue mustBe odo.sdo.get.stringValue

  }


 

}
