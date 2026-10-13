package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import java.time.{Instant, OffsetDateTime, ZoneId, ZoneOffset, ZonedDateTime}
import java.util.UUID

object ExtraTypesSpec {
  case class Money(amount: BigDecimal, count: BigInt, jAmount: java.math.BigDecimal, jCount: java.math.BigInteger)
  case class Ids(id: UUID, at: Instant, offset: OffsetDateTime, zoned: ZonedDateTime, maybe: Option[UUID])
  case class Unsupported(date: java.util.Date)
}

class ExtraTypesSpec extends AnyFlatSpec {
  import ExtraTypesSpec._

  val mpjsons = new MPJsons

  "Serializer" must "write BigDecimal and BigInt as exact JSON numbers" in {
    val money = Money(BigDecimal("12345678901234567890.123456789"), BigInt("98765432109876543210"),
      new java.math.BigDecimal("0.10"), new java.math.BigInteger("-7"))
    val json = mpjsons.serialize(money)
    json mustBe """{"amount":12345678901234567890.123456789,"count":98765432109876543210,"jAmount":0.10,"jCount":-7}"""
    mpjsons.deserialize[Money](json) mustBe money
    mpjsons.deserialize[BigDecimal]("1.5e3") mustBe BigDecimal("1.5e3")
    mpjsons.deserialize[BigInt]("  42 ") mustBe BigInt(42)
    mpjsons.serialize(BigDecimal("1E+10")) mustBe "1E+10"
    intercept[JsonInnerException] { mpjsons.deserialize[BigDecimal]("\"1\"") }.getMessage must include("Expected a number")
  }

  "Serializer" must "write UUID and the zoned java.time types as strings" in {
    val ids = Ids(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"), Instant.parse("2023-07-23T10:15:30.123Z"),
      OffsetDateTime.of(2023, 7, 23, 10, 15, 30, 0, ZoneOffset.ofHours(2)),
      ZonedDateTime.of(2023, 7, 23, 10, 15, 30, 0, ZoneId.of("Europe/Warsaw")), Some(UUID.fromString("00000000-0000-0000-0000-000000000001")))
    val json = mpjsons.serialize(ids)
    json mustBe """{"id":"123e4567-e89b-12d3-a456-426614174000","at":"2023-07-23T10:15:30.123Z","offset":"2023-07-23T10:15:30+02:00","zoned":"2023-07-23T10:15:30+02:00[Europe\/Warsaw]","maybe":{"value":"00000000-0000-0000-0000-000000000001"}}"""
    mpjsons.deserialize[Ids](json) mustBe ids
    new MPJsons(MPJsonsConfig(escapeSlash = false)).serialize(ids) must include(""""zoned":"2023-07-23T10:15:30+02:00[Europe/Warsaw]"""")
    val compact = new MPJsons(MPJsonsConfig(compactOptions = true))
    compact.deserialize[Ids](compact.serialize(ids)) mustBe ids
    intercept[JsonInnerException] { mpjsons.deserialize[UUID]("\"not-a-uuid\"") }
  }

  "Serializer" must "fail fast for java types without a built-in serializer or a registered converter" in {
    val e = intercept[IllegalArgumentException] { mpjsons.serialize(Unsupported(new java.util.Date(0))) }
    e.getMessage must include("java.util.Date")
    e.getMessage must include("registerConverter")
    val d = intercept[JsonInnerException] { mpjsons.deserialize[Unsupported]("""{"date":{}}""") }
    d.getMessage must include("java.util.Date")
    d.getMessage must include("registerConverter")
  }

  "Serializer" must "still allow a registered converter for a java type" in {
    val custom = new MPJsons
    custom.registerConverter[java.util.Date](
      sf => (obj: java.util.Date, jsonBuilder: StringBuilder) => { jsonBuilder.append(obj.getTime) },
      df => (jsonIterator: impl.StringIterator) => new java.util.Date(impl.deserializer.values.LongDeserializer.deserialize(jsonIterator)))
    custom.serialize(Unsupported(new java.util.Date(5))) mustBe """{"date":5}"""
    custom.deserialize[Unsupported]("""{"date":5}""") mustBe Unsupported(new java.util.Date(5))
  }
}
