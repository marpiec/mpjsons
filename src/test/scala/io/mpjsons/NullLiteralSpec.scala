package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object NullLiteralSpec {
  case class Nested(v: Int)
  // every reference field declared nullable
  case class Refs(@nullable s: String, o: Option[String], @nullable l: List[String], @nullable m: Map[String, String], @nullable n: Nested, i: Int)
  // nothing declared nullable
  case class Strict(s: String, l: List[String], n: Nested, i: Int)
  case class Prims(i: Int, b: Boolean, d: Double)
  case class Opts(a: Option[Int], b: Option[String], c: Option[Option[Int]])
  case class Tup(t: (String, Int), e: Either[String, String])
  case class Boxed(@nullable i: java.lang.Integer, b: java.lang.Boolean)
  case class Profile(@nullable name: String, nickname: Option[String], city: String, age: Int)

  class Bean {
    @nullable var s: String = "init"
    var o: Option[Int] = Some(1)
  }
}

/**
 * Under the default NullPolicy.Fail a JSON null is accepted only where nullability is declared: `@nullable` fields
 * and Option values. NullPolicy.Omit and WriteNull accept nulls anywhere (and write null elements).
 */
class NullLiteralSpec extends AnyFlatSpec {
  import NullLiteralSpec._

  val mpjsons = new MPJsons
  val tolerant = new MPJsons(nullPolicy = NullPolicy.Omit)
  val writer = new MPJsons(nullPolicy = NullPolicy.WriteNull)

  // ---------------------------------------------------------------- reading

  "Deserializer" must "read null into @nullable fields as null and into Option fields as None" in {
    val refs = mpjsons.deserialize[Refs]("""{"s":null,"o":null,"l":null,"m":null,"n":null,"i":1}""")
    refs mustBe Refs(null, None, null, null, null, 1)
    mpjsons.deserialize[Refs]("""{ "s" : null , "i" : 2 }""") mustBe Refs(null, None, null, null, null, 2)
    mpjsons.deserialize[Boxed]("""{"i":null,"b":true}""") mustBe Boxed(null, true)
  }

  "Deserializer" must "reject null for fields that are not declared nullable, naming the field" in {
    val e = intercept[JsonInnerException] { mpjsons.deserialize[Strict]("""{"s":null,"i":1}""") }
    e.getMessage must include("null is not allowed")
    e.getMessage must include("'s'")
    e.getMessage must include("@nullable")
    intercept[JsonInnerException] { mpjsons.deserialize[Strict]("""{"l":null}""") }.getMessage must include("'l'")
    intercept[JsonInnerException] { mpjsons.deserialize[Strict]("""{"n":null}""") }.getMessage must include("'n'")
    intercept[JsonInnerException] { mpjsons.deserialize[Boxed]("""{"i":null,"b":null}""") }.getMessage must include("'b'")
  }

  "Deserializer" must "reject null for primitive fields even when annotated" in {
    val e = intercept[JsonInnerException] { mpjsons.deserialize[Prims]("""{"i":null,"b":true,"d":1.5}""") }
    e.getMessage must include("null is not allowed")
    e.getMessage must include("'i'")
    intercept[JsonInnerException] { writer.deserialize[Prims]("""{"i":1,"b":null,"d":1.5}""") }.getMessage must include("'b'")
    intercept[JsonInnerException] { tolerant.deserialize[Prims]("""{"i":1,"b":true,"d":null}""") }.getMessage must include("'d'")
  }

  "Deserializer" must "accept null anywhere under NullPolicy.Omit and WriteNull" in {
    tolerant.deserialize[Strict]("""{"s":null,"l":null,"n":null,"i":1}""") mustBe Strict(null, null, null, 1)
    writer.deserialize[Strict]("""{"s":null,"l":null,"n":null,"i":1}""") mustBe Strict(null, null, null, 1)
    tolerant.deserialize[List[String]]("""[null,"a"]""") mustBe List(null, "a")
    tolerant.deserialize[Map[String, String]]("""[["k",null],[null,"v"]]""") mustBe Map("k" -> null, (null: String) -> "v")
    tolerant.deserialize[Tup]("""{"t":[null,1],"e":{"left":null}}""") mustBe Tup((null, 1), Left(null))
    tolerant.deserialize[Opts]("""{"b":{"value":null}}""") mustBe Opts(None, Some(null), None)
    tolerant.deserialize[String]("null") mustBe null
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

  "Deserializer" must "read null only into Option elements under the default policy" in {
    mpjsons.deserialize[Vector[Option[Int]]]("""[null,{"value":1},{}]""") mustBe Vector(None, Some(1), None)
    mpjsons.deserialize[Map[String, Option[String]]]("""[["k",null]]""") mustBe Map("k" -> None)
    mpjsons.deserialize[Opts]("""{"c":{"value":null}}""") mustBe Opts(None, None, Some(None))
    mpjsons.deserialize[Option[Int]]("null") mustBe None

    intercept[JsonInnerException] { mpjsons.deserialize[List[String]]("""[null,"a"]""") }.getMessage must include("null is not allowed")
    intercept[JsonInnerException] { mpjsons.deserialize[List[Int]]("[1,null]") }.getMessage must include("null is not allowed")
    intercept[JsonInnerException] { mpjsons.deserialize[Map[String, String]]("""[["k",null]]""") }
    intercept[JsonInnerException] { mpjsons.deserialize[Tup]("""{"t":[null,1],"e":{"left":"x"}}""") }
    intercept[JsonInnerException] { mpjsons.deserialize[Tup]("""{"t":["a",1],"e":{"left":null}}""") }
    intercept[JsonInnerException] { mpjsons.deserialize[Opts]("""{"b":{"value":null}}""") }
    intercept[JsonInnerException] { mpjsons.deserialize[String]("null") }.getMessage must include("null is not allowed")
    intercept[JsonInnerException] { mpjsons.deserialize[Int]("null") }.getMessage must include("null is not allowed")
    intercept[JsonInnerException] { mpjsons.buildStaticDeserializer[String].deserialize("null") }
  }

  "Deserializer" must "reject tokens that only start like null" in {
    intercept[JsonInnerException] { mpjsons.deserialize[Refs]("""{"s":nul}""") }
    intercept[JsonInnerException] { mpjsons.deserialize[Refs]("""{"s":nothing}""") }
  }

  "Deserializer" must "still skip null values of unknown fields in lenient mode" in {
    new MPJsons(ignoreNonExistingFields = true).deserialize[Nested]("""{"zzz":null,"v":3}""") mustBe Nested(3)
  }

  // ---------------------------------------------------------------- writing

  "Serializer" must "fail on null fields by default and omit @nullable ones" in {
    intercept[IllegalArgumentException] { mpjsons.serialize(Strict(null, Nil, Nested(1), 1)) }
    mpjsons.serialize(Refs(null, None, null, null, null, 1)) mustBe """{"o":{},"i":1}"""
  }

  "Serializer" must "omit null fields with NullPolicy.Omit (and ignoreNullFields)" in {
    tolerant.serialize(Strict(null, null, null, 1)) mustBe """{"i":1}"""
    new MPJsons(ignoreNullFields = true).serialize(Strict(null, null, null, 1)) mustBe """{"i":1}"""
  }

  "Serializer" must "write null fields with NullPolicy.WriteNull" in {
    writer.serialize(Strict(null, null, null, 1)) mustBe """{"s":null,"l":null,"n":null,"i":1}"""
    writer.serialize(Refs(null, None, null, null, null, 1)) mustBe """{"s":null,"o":{},"l":null,"m":null,"n":null,"i":1}"""
    writer.serialize(Boxed(null, null)) mustBe """{"i":null,"b":null}"""
    writer.deserialize[Strict](writer.serialize(Strict(null, null, null, 1))) mustBe Strict(null, null, null, 1)
  }

  "Serializer" must "reject null elements under the default policy and write them under the others" in {
    intercept[IllegalArgumentException] { mpjsons.serialize(List("a", null)) }.getMessage must include("NullPolicy.Fail")
    intercept[IllegalArgumentException] { mpjsons.serialize(Vector[java.lang.Integer](1, null)) }
    intercept[IllegalArgumentException] { mpjsons.serialize(Array[String](null)) }
    intercept[IllegalArgumentException] { mpjsons.serialize(Map("k" -> (null: String))) }
    intercept[IllegalArgumentException] { mpjsons.serialize[Option[String]](Some(null)) }
    intercept[IllegalArgumentException] { mpjsons.serialize[(String, Int)]((null, 1)) }
    intercept[IllegalArgumentException] { mpjsons.serialize[Either[String, Int]](Left(null)) }
    intercept[IllegalArgumentException] { mpjsons.serialize[String](null) }
    intercept[IllegalArgumentException] { mpjsons.buildStaticSerializer[String].serialize(null) }

    tolerant.serialize(List("a", null)) mustBe """["a",null]"""
    writer.serialize(Vector[java.lang.Integer](1, null)) mustBe """[1,null]"""
    writer.serialize(Array[String](null)) mustBe """[null]"""
    writer.serialize(Map("k" -> (null: String))) mustBe """[["k",null]]"""
    writer.serialize[Option[String]](Some(null)) mustBe """{"value":null}"""
    writer.serialize[(String, Int)]((null, 1)) mustBe """[null,1]"""
    writer.serialize[Either[String, Int]](Left(null)) mustBe """{"left":null}"""
    writer.serialize[String](null) mustBe "null"
    writer.serialize[Refs](null) mustBe "null"
    writer.buildStaticSerializer[String].serialize(null) mustBe "null"
  }

  "README examples" must "hold" in {
    mpjsons.deserialize[Profile]("""{"name":null,"nickname":null,"city":"Oslo","age":3}""") mustBe Profile(null, None, "Oslo", 3)
    mpjsons.deserialize[Profile]("""{"name":"Ann","city":"Oslo","age":3}""") mustBe Profile("Ann", None, "Oslo", 3)
    intercept[JsonInnerException] { mpjsons.deserialize[Profile]("""{"name":"Ann","city":null,"age":3}""") }.getMessage must include("'city'")
    mpjsons.serialize(Profile(null, None, "Oslo", 3)) mustBe """{"nickname":{},"city":"Oslo","age":3}"""
    intercept[IllegalArgumentException] { mpjsons.serialize(List("a", null)) }
    tolerant.serialize(Profile("Ann", None, null, 3)) mustBe """{"name":"Ann","nickname":{},"age":3}"""
    writer.serialize(Profile("Ann", None, null, 3)) mustBe """{"name":"Ann","nickname":{},"city":null,"age":3}"""
    writer.serialize(List("a", null)) mustBe """["a",null]"""
  }

  "Serializer" must "always write None and Option elements without a null value" in {
    mpjsons.serialize(List(Some(1), None)) mustBe """[{"value":1},{}]"""
    new MPJsons(MPJsonsConfig(compactOptions = true)).serialize(List(Some(1), None)) mustBe "[1,null]"
  }
}
