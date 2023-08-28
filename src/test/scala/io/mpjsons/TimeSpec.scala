package io.mpjsons


import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import java.time.{LocalDate, LocalDateTime, LocalTime}

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
}
