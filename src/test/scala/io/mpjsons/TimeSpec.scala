package io.mpjsons


import io.mpjsons.impl.JsonInnerException
import io.mpjsons.impl.StringIterator
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import java.time.{Duration, LocalDate, LocalDateTime, LocalTime}

class TimeSpec extends AnyFlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle time serialization" in {

      val time = LocalTime.of(12, 30, 17, 125)

      val serialized = mpjsons.serialize(time)

      serialized mustBe """{"hour":12,"minute":30,"second":17,"nano":125}"""
  }

  "Serializer" must "handle date serialization" in {

      val date = LocalDate.of(2023, 7, 23)

      val serialized = mpjsons.serialize(date)

      serialized mustBe """{"year":2023,"month":7,"day":23}"""
  }

  "Serializer" must "handle dateTime serialization" in {

    val dateTime = LocalDateTime.of(2023, 8, 23, 14, 30, 17, 125)

    val serialized = mpjsons.serialize(dateTime)

    serialized mustBe """{"date":{"year":2023,"month":8,"day":23},"time":{"hour":14,"minute":30,"second":17,"nano":125}}"""
  }

  "Serializer" must "handle duration deserialization" in {

    val duration = java.time.Duration.ofSeconds(123, 456)

    val serialized = mpjsons.serialize(duration)

    serialized mustBe """{"seconds":123,"nanos":456}"""

  }

  "Deserializer" must "handle time deserialization" in {

    val json = """ {  "hour" : 12 , "minute" : 30 , "second" :   17 , "nano" : 125 }   """

    val deserialized = mpjsons.deserialize[LocalTime](json)

    deserialized mustBe LocalTime.of(12, 30, 17, 125)
  }


  "Deserializer" must "handle date deserialization" in {

    val json = """ {  "year" : 2023 , "month" : 7 , "day" :   23 }   """

    val deserialized = mpjsons.deserialize[LocalDate](json)

    deserialized mustBe LocalDate.of(2023, 7, 23)
  }


  "Deserializer" must "handle dateTime deserialization" in {

    val json = """ { "date": {  "year" : 2023 , "month" : 8 , "day" :   23 }, "time" :  {  "hour" : 14 , "minute" : 30 , "second" :   17 , "nano" : 125 } }  """

    val deserialized = mpjsons.deserialize[LocalDateTime](json)

    deserialized mustBe LocalDateTime.of(2023, 8, 23, 14, 30, 17, 125)
  }

  "Deserializer" must "handle duration deserialization" in {

    val json = """ { "seconds"   :   125  ,   "nanos"  :  457  }  """

    val deserialized = mpjsons.deserialize[java.time.Duration](json)

    deserialized mustBe java.time.Duration.ofSeconds(125, 457)
  }

  "Deserializer" must "handle negative duration deserialization" in {

    val json = """ { "seconds"   :   -125  ,   "nanos"  :  457  }  """

    val deserialized = mpjsons.deserialize[java.time.Duration](json)

    deserialized mustBe java.time.Duration.ofSeconds(-125, 457)
  }

  "Deserializer" must "accept time fields in any order" in {
    mpjsons.deserialize[LocalDate]("""{"day":23,"year":2023,"month":7}""") mustBe LocalDate.of(2023, 7, 23)
    mpjsons.deserialize[LocalTime]("""{"nano":1,"second":2,"minute":3,"hour":4}""") mustBe LocalTime.of(4, 3, 2, 1)
    mpjsons.deserialize[LocalDateTime]("""{"time":{"hour":4,"minute":3,"second":2,"nano":1},"date":{"year":2023,"month":7,"day":23}}""") mustBe
      LocalDateTime.of(2023, 7, 23, 4, 3, 2, 1)
    mpjsons.deserialize[Duration]("""{"nanos":2,"seconds":1}""") mustBe Duration.ofSeconds(1, 2)
  }

  "Deserializer" must "report which time field is missing" in {
    intercept[JsonInnerException] { mpjsons.deserialize[LocalDate]("""{"year":2023,"month":7}""") }.getMessage must include("day")
    intercept[JsonInnerException] { mpjsons.deserialize[LocalTime]("""{"hour":1,"minute":2,"second":3}""") }.getMessage must include("nano")
    intercept[JsonInnerException] { mpjsons.deserialize[LocalDateTime]("""{"date":{"year":2023,"month":7,"day":23}}""") }.getMessage must include("time")
    intercept[JsonInnerException] { mpjsons.deserialize[Duration]("""{"seconds":1}""") }.getMessage must include("nanos")
    intercept[JsonInnerException] { mpjsons.deserialize[LocalDate]("{}") }.getMessage must include("year")
  }

  "Deserializer" must "reject unknown time fields" in {
    intercept[JsonInnerException] { mpjsons.deserialize[LocalDate]("""{"year":2023,"month":7,"day":23,"hour":1}""") }.getMessage must include("hour")
  }

  "Converter registration" must "override the built-in Duration format like the other time types" in {
    val custom = new MPJsons
    custom.registerConverter[Duration](
      sf => (obj: Duration, jsonBuilder: StringBuilder) => { jsonBuilder.append('"').append(obj.toString).append('"') },
      df => (jsonIterator: StringIterator) => Duration.parse(impl.deserializer.values.StringDeserializer.deserialize(jsonIterator)))

    custom.serialize(Duration.ofMinutes(2)) mustBe "\"PT2M\""
    custom.deserialize[Duration]("\"PT2M\"") mustBe Duration.ofMinutes(2)
  }
}
