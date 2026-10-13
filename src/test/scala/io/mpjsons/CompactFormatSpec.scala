package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import java.time.{Duration, LocalDate, LocalDateTime, LocalTime}

object CompactFormatSpec {
  case class Url(path: String)
  case class Nested(v: Int)
  case class Opts(a: Option[Int], b: Option[String], c: Option[Nested], d: Option[List[Int]], e: Option[Option[Int]])
  case class Stamp(at: LocalDateTime, d: Option[Duration])
}

/** Opt-in format levers of MPJsonsConfig; defaults keep the historical format. */
class CompactFormatSpec extends AnyFlatSpec {
  import CompactFormatSpec._

  val legacy = new MPJsons
  val noSlash = new MPJsons(MPJsonsConfig(escapeSlash = false))
  val compactOptions = new MPJsons(MPJsonsConfig(compactOptions = true))

  "MPJsons" must "still accept the historical constructor arguments" in {
    new MPJsons(true).config.ignoreNonExistingFields mustBe true
    new MPJsons(ignoreNullFields = true).config.nullPolicy mustBe NullPolicy.Omit
    new MPJsons(nullPolicy = NullPolicy.WriteNull).config.nullPolicy mustBe NullPolicy.WriteNull
    legacy.config mustBe MPJsonsConfig()
  }

  "escapeSlash" must "control whether / is escaped and never affect reading" in {
    legacy.serialize(Url("a/b")) mustBe """{"path":"a\/b"}"""
    noSlash.serialize(Url("a/b")) mustBe """{"path":"a/b"}"""
    legacy.deserialize[Url]("""{"path":"a\/b"}""") mustBe Url("a/b")
    legacy.deserialize[Url]("""{"path":"a/b"}""") mustBe Url("a/b")
    noSlash.deserialize[Url]("""{"path":"a\/b"}""") mustBe Url("a/b")
    noSlash.serialize("x/y\"z") mustBe "\"x/y\\\"z\""
  }

  "compactOptions" must "write Some inline and omit None fields" in {
    compactOptions.serialize(Opts(Some(1), None, Some(Nested(2)), Some(List(3)), Some(None))) mustBe
      """{"a":1,"c":{"v":2},"d":[3],"e":{}}"""
    compactOptions.serialize(Opts(Some(1), Some("s"), None, None, Some(Some(4)))) mustBe
      """{"a":1,"b":"s","e":{"value":4}}"""
    compactOptions.serialize(Opts(None, None, None, None, None)) mustBe "{}"
    legacy.serialize(Opts(Some(1), None, None, None, None)) mustBe """{"a":{"value":1},"b":{},"c":{},"d":{},"e":{}}"""
  }

  "compactOptions" must "round trip through its own format" in {
    val values = List(
      Opts(Some(1), None, Some(Nested(2)), Some(List(3)), Some(None)),
      Opts(Some(1), Some("s"), None, None, Some(Some(4))),
      Opts(None, None, None, None, None))
    for (value <- values) {
      compactOptions.deserialize[Opts](compactOptions.serialize(value)) mustBe value
    }
  }

  "compactOptions" must "read the historical form wherever it is distinguishable from an inline value" in {
    compactOptions.deserialize[Opts]("""{"a":{"value":1},"b":{},"d":{"value":[3]}}""") mustBe
      Opts(Some(1), None, None, Some(List(3)), None)
    // Option of a bean or of an Option: '{' must be the inline element in compact mode, so the historical
    // outer form is not recognised for these element types.
    intercept[JsonInnerException] { compactOptions.deserialize[Opts]("""{"c":{"value":{"v":2}}}""") }
    intercept[JsonInnerException] { compactOptions.deserialize[Opts]("""{"e":{"value":{"value":1}}}""") }
    compactOptions.deserialize[Opts]("""{"e":{"value":1}}""") mustBe Opts(None, None, None, None, Some(Some(1)))
  }

  "the historical format" must "read inline values where they cannot be the object form" in {
    legacy.deserialize[Opts]("""{"a":1,"b":"s","d":[3]}""") mustBe Opts(Some(1), Some("s"), None, Some(List(3)), None)
    legacy.deserialize[Opts]("""{"a":null,"c":{"value":{"v":2}}}""") mustBe Opts(None, None, Some(Nested(2)), None, None)
  }

  "compactOptions" must "apply to elements and top-level values" in {
    compactOptions.serialize(List(Some(1), None)) mustBe "[1,null]"
    compactOptions.deserialize[List[Option[Int]]]("[1,null]") mustBe List(Some(1), None)
    compactOptions.serialize(Map("k" -> Some("v"), "n" -> None)) mustBe """[["k","v"],["n",null]]"""
    compactOptions.deserialize[Map[String, Option[String]]]("""[["k","v"],["n",null]]""") mustBe Map("k" -> Some("v"), "n" -> None)
    compactOptions.serialize(Some(1)) mustBe "1"
    compactOptions.serialize[Option[Int]](None) mustBe "null"
    compactOptions.deserialize[Option[Int]]("1") mustBe Some(1)
    compactOptions.deserialize[Option[Int]]("null") mustBe None
    compactOptions.deserialize[Option[Nested]]("""{"v":5}""") mustBe Some(Nested(5))
  }

  val mapsAsObjects = new MPJsons(MPJsonsConfig(mapsAsObjects = true))

  "mapsAsObjects" must "write maps with scalar keys as JSON objects" in {
    mapsAsObjects.serialize(Map("a" -> 1, "b" -> 2)) mustBe """{"a":1,"b":2}"""
    mapsAsObjects.serialize(Map(1 -> "x", 2 -> "y")) mustBe """{"1":"x","2":"y"}"""
    mapsAsObjects.serialize(Map(true -> List(1))) mustBe """{"true":[1]}"""
    mapsAsObjects.serialize(Map('c' -> 1.5)) mustBe """{"c":1.5}"""
    mapsAsObjects.serialize(Map("q\"uote" -> (null: String))) mustBe """{"q\"uote":null}"""
    mapsAsObjects.serialize(Map(Nested(1) -> "bean")) mustBe """[[{"v":1},"bean"]]"""
    mapsAsObjects.serialize(scala.collection.mutable.Map("m" -> 1)) mustBe """{"m":1}"""
    legacy.serialize(Map("a" -> 1)) mustBe """[["a",1]]"""
  }

  "mapsAsObjects" must "round trip and read both forms in both configurations" in {
    val maps = (Map("a" -> 1, "b" -> 2), Map(1L -> "x"), Map(true -> 1), Map('c' -> 2), Map(Nested(1) -> "bean"))
    mapsAsObjects.deserialize[(Map[String, Int], Map[Long, String], Map[Boolean, Int], Map[Char, Int], Map[Nested, String])](mapsAsObjects.serialize(maps)) mustBe maps
    legacy.deserialize[Map[String, Int]]("""{"a":1,"b":2}""") mustBe Map("a" -> 1, "b" -> 2)
    legacy.deserialize[Map[Int, String]]("""{ 1 : "x", "2":"y" }""") mustBe Map(1 -> "x", 2 -> "y")
    mapsAsObjects.deserialize[Map[String, Int]]("""[["a",1]]""") mustBe Map("a" -> 1)
    mapsAsObjects.deserialize[scala.collection.immutable.ListMap[String, Int]]("""{"z":1,"a":2}""").toList mustBe List("z" -> 1, "a" -> 2)
    mapsAsObjects.deserialize[Map[String, Option[Int]]]("""{"a":null}""") mustBe Map("a" -> None)
    intercept[JsonInnerException] { legacy.deserialize[Map[Nested, String]]("""{"x":"y"}""") }.getMessage must include("cannot be read from a JSON object")
    intercept[IllegalArgumentException] { mapsAsObjects.serialize(Map((null: String) -> 1)) }
  }

  "mapsAsObjects" must "combine with compactOptions for Option[Map]" in {
    val both = new MPJsons(MPJsonsConfig(compactOptions = true, mapsAsObjects = true))
    both.serialize(Option(Map("a" -> 1))) mustBe """{"a":1}"""
    both.deserialize[Option[Map[String, Int]]]("""{"a":1}""") mustBe Some(Map("a" -> 1))
    both.deserialize[Option[Map[String, Int]]]("null") mustBe None
  }

  val isoTime = new MPJsons(MPJsonsConfig(isoTime = true))

  "isoTime" must "write java.time values as ISO-8601 strings" in {
    isoTime.serialize(LocalDate.of(2023, 7, 23)) mustBe "\"2023-07-23\""
    isoTime.serialize(LocalTime.of(12, 30, 17, 125)) mustBe "\"12:30:17.000000125\""
    isoTime.serialize(LocalDateTime.of(2023, 8, 23, 14, 30)) mustBe "\"2023-08-23T14:30\""
    isoTime.serialize(Duration.ofSeconds(123, 456)) mustBe "\"PT2M3.000000456S\""
    legacy.serialize(LocalDate.of(2023, 7, 23)) mustBe """{"year":2023,"month":7,"day":23}"""
  }

  "isoTime" must "read both forms in both configurations" in {
    isoTime.deserialize[LocalDate]("\"2023-07-23\"") mustBe LocalDate.of(2023, 7, 23)
    isoTime.deserialize[LocalDate]("""{"year":2023,"month":7,"day":23}""") mustBe LocalDate.of(2023, 7, 23)
    legacy.deserialize[LocalTime]("\"12:30:17.000000125\"") mustBe LocalTime.of(12, 30, 17, 125)
    legacy.deserialize[LocalDateTime]("\"2023-08-23T14:30\"") mustBe LocalDateTime.of(2023, 8, 23, 14, 30)
    legacy.deserialize[Duration](" \"PT2M3.000000456S\" ") mustBe Duration.ofSeconds(123, 456)
    intercept[JsonInnerException] { isoTime.deserialize[LocalDate]("\"not a date\"") }
  }

  "isoTime" must "combine with compactOptions" in {
    val both = new MPJsons(MPJsonsConfig(isoTime = true, compactOptions = true))
    val stamp = Stamp(LocalDateTime.of(2023, 8, 23, 14, 30), Some(Duration.ofMinutes(1)))
    both.serialize(stamp) mustBe """{"at":"2023-08-23T14:30","d":"PT1M"}"""
    both.deserialize[Stamp](both.serialize(stamp)) mustBe stamp
    both.deserialize[Stamp]("""{"at":"2023-08-23T14:30"}""") mustBe stamp.copy(d = None)
    legacy.deserialize[Stamp]("""{"at":"2023-08-23T14:30","d":{"value":"PT1M"}}""") mustBe stamp
    legacy.serialize(stamp) mustBe """{"at":{"date":{"year":2023,"month":8,"day":23},"time":{"hour":14,"minute":30,"second":0,"nano":0}},"d":{"value":{"seconds":60,"nanos":0}}}"""
  }

  "compactOptions" must "keep the historical format for everything else" in {
    val obj = Opts(Some(1), Some("s"), Some(Nested(2)), Some(List(3)), Some(Some(4)))
    compactOptions.serialize(Nested(1)) mustBe legacy.serialize(Nested(1))
    compactOptions.serialize(List(1, 2)) mustBe legacy.serialize(List(1, 2))
    compactOptions.serialize(Url("a/b")) mustBe legacy.serialize(Url("a/b"))
    legacy.deserialize[Opts](legacy.serialize(obj)) mustBe obj
  }
}
