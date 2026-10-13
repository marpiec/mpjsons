package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import scala.annotation.meta.field

object NullLiteralSpec {
  case class Nested(v: Int)
  case class Refs(s: String, o: Option[String], l: List[String], m: Map[String, String], n: Nested, i: Int)
  case class Prims(i: Int, b: Boolean, d: Double)
  case class Opts(a: Option[Int], b: Option[String], c: Option[Option[Int]])
  case class Tup(t: (String, Int), e: Either[String, String])
  case class Boxed(i: java.lang.Integer, b: java.lang.Boolean)
  case class WithNullable(@(nullable @field) a: String, b: String)

  class Bean {
    var s: String = "init"
    var o: Option[Int] = Some(1)
  }
}

class NullLiteralSpec extends AnyFlatSpec {
  import NullLiteralSpec._

  val mpjsons = new MPJsons

  // ---------------------------------------------------------------- reading

  "Deserializer" must "read null into reference fields as null and into Option fields as None" in {
    val refs = mpjsons.deserialize[Refs]("""{"s":null,"o":null,"l":null,"m":null,"n":null,"i":1}""")
    refs mustBe Refs(null, None, null, null, null, 1)
    mpjsons.deserialize[Boxed]("""{"i":null,"b":null}""") mustBe Boxed(null, null)
    mpjsons.deserialize[Refs]("""{ "s" : null , "i" : 2 }""") mustBe Refs(null, None, null, null, null, 2)
  }

  "Deserializer" must "reject null for primitive fields with a message naming the field" in {
    val e = intercept[JsonInnerException] { mpjsons.deserialize[Prims]("""{"i":null,"b":true,"d":1.5}""") }
    e.getMessage must include("null is not allowed")
    e.getMessage must include("'i'")
    intercept[JsonInnerException] { mpjsons.deserialize[Prims]("""{"i":1,"b":null,"d":1.5}""") }.getMessage must include("'b'")
    intercept[JsonInnerException] { mpjsons.deserialize[Prims]("""{"i":1,"b":true,"d":null}""") }.getMessage must include("'d'")
  }

  "Deserializer" must "read absent Option fields as None" in {
    mpjsons.deserialize[Opts]("{}") mustBe Opts(None, None, None)
    mpjsons.deserialize[Opts]("""{"a":{"value":1}}""") mustBe Opts(Some(1), None, None)
  }

  "Deserializer" must "not override Option defaults set by a no-arg constructor" in {
    val fromEmpty = mpjsons.deserialize[Bean]("{}")
    fromEmpty.s mustBe "init"
    fromEmpty.o mustBe Some(1)

    val fromNulls = mpjsons.deserialize[Bean]("""{"s":null,"o":null}""")
    fromNulls.s mustBe null
    fromNulls.o mustBe None
  }

  "Deserializer" must "read null elements of collections, maps, tuples, Either and Option" in {
    mpjsons.deserialize[List[String]]("""[null,"a",null]""") mustBe List(null, "a", null)
    mpjsons.deserialize[Vector[Option[Int]]]("""[null,{"value":1},{}]""") mustBe Vector(None, Some(1), None)
    mpjsons.deserialize[Map[String, String]]("""[["k",null],[null,"v"]]""") mustBe Map("k" -> null, (null: String) -> "v")
    mpjsons.deserialize[Opts]("""{"c":{"value":null},"b":{"value":null}}""") mustBe Opts(None, Some(null), Some(None))
    mpjsons.deserialize[Tup]("""{"t":[null,1],"e":{"left":null}}""") mustBe Tup((null, 1), Left(null))
    mpjsons.deserialize[Array[String]]("""["x",null]""").toList mustBe List("x", null)
  }

  "Deserializer" must "reject null elements of primitive collections" in {
    intercept[JsonInnerException] { mpjsons.deserialize[List[Int]]("[1,null]") }.getMessage must include("null is not allowed")
    intercept[JsonInnerException] { mpjsons.deserialize[Map[Int, String]]("""[[null,"a"]]""") }.getMessage must include("null is not allowed")
  }

  "Deserializer" must "read a top-level null" in {
    mpjsons.deserialize[String]("null") mustBe null
    mpjsons.deserialize[Refs](" null ") mustBe null
    mpjsons.deserialize[Option[Int]]("null") mustBe None
    intercept[JsonInnerException] { mpjsons.deserialize[Int]("null") }.getMessage must include("null is not allowed")
    mpjsons.buildStaticDeserializer[String].deserialize("null") mustBe null
  }

  "Deserializer" must "reject tokens that only start like null" in {
    intercept[JsonInnerException] { mpjsons.deserialize[Refs]("""{"s":nul}""") }
    intercept[JsonInnerException] { mpjsons.deserialize[Refs]("""{"s":nothing}""") }
  }

  "Deserializer" must "still skip null values of unknown fields in lenient mode" in {
    new MPJsons(ignoreNonExistingFields = true).deserialize[Nested]("""{"zzz":null,"v":3}""") mustBe Nested(3)
  }

  // ---------------------------------------------------------------- writing

  "Serializer" must "fail on null fields by default and omit nullable ones" in {
    intercept[IllegalArgumentException] { mpjsons.serialize(Refs(null, None, Nil, Map.empty, Nested(1), 1)) }
    mpjsons.serialize(WithNullable(null, "b")) mustBe """{"b":"b"}"""
  }

  "Serializer" must "omit null fields with NullPolicy.Omit (and ignoreNullFields)" in {
    new MPJsons(nullPolicy = NullPolicy.Omit).serialize(Refs(null, None, null, null, null, 1)) mustBe """{"o":{},"i":1}"""
    new MPJsons(ignoreNullFields = true).serialize(Refs(null, None, null, null, null, 1)) mustBe """{"o":{},"i":1}"""
  }

  "Serializer" must "write null fields with NullPolicy.WriteNull" in {
    val writer = new MPJsons(nullPolicy = NullPolicy.WriteNull)
    writer.serialize(Refs(null, None, null, null, null, 1)) mustBe """{"s":null,"o":{},"l":null,"m":null,"n":null,"i":1}"""
    writer.serialize(WithNullable(null, null)) mustBe """{"a":null,"b":null}"""
    writer.serialize(Boxed(null, null)) mustBe """{"i":null,"b":null}"""
    writer.deserialize[Refs](writer.serialize(Refs(null, None, null, null, null, 1))) mustBe Refs(null, None, null, null, null, 1)
  }

  "Serializer" must "write null elements of collections, maps, tuples, Either and Some as null" in {
    mpjsons.serialize(List("a", null)) mustBe """["a",null]"""
    mpjsons.serialize(Vector[java.lang.Integer](1, null)) mustBe """[1,null]"""
    mpjsons.serialize(Array[String](null)) mustBe """[null]"""
    mpjsons.serialize(Map("k" -> (null: String))) mustBe """[["k",null]]"""
    mpjsons.serialize[Option[String]](Some(null)) mustBe """{"value":null}"""
    mpjsons.serialize[(String, Int)]((null, 1)) mustBe """[null,1]"""
    mpjsons.serialize[Either[String, Int]](Left(null)) mustBe """{"left":null}"""
  }

  "Serializer" must "write a top-level null as null" in {
    mpjsons.serialize[String](null) mustBe "null"
    mpjsons.serialize[Refs](null) mustBe "null"
    mpjsons.buildStaticSerializer[String].serialize(null) mustBe "null"
  }
}
