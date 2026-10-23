package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import scala.annotation.meta.field

object JsonNameSpec {
  // Annotations on case class parameters, without any @field meta-annotation.
  case class Renamed(@jsonName("n") name: String, @jsonName("a") age: Int, plain: Boolean)

  // Annotation on a plain var.
  class Bean {
    @jsonName("v") var value: Int = 0
    var other: String = _
  }

  // The explicit @field form keeps working.
  case class Meta(@(jsonName @field)("m") first: String)

  // Inherited renamed field.
  class Base { @jsonName("id") var identifier: Long = 0 }
  class Derived extends Base { @jsonName("l") var label: String = _ }

  case class Duplicate(@jsonName("x") a: Int, @jsonName("x") b: Int)
  case class Swapped(@jsonName("b") a: Int, @jsonName("a") b: Int)
  case class ClashWithPlain(@jsonName("b") a: Int, b: Int)

  // @nullable also no longer needs the @field meta-annotation.
  case class NullableNoMeta(@nullable a: String, b: String)

  case class Generic[T](@jsonName("v") value: T, @jsonName("vs") values: List[T])
}

class JsonNameSpec extends AnyFlatSpec {
  import JsonNameSpec._

  val mpjsons = new MPJsons

  "@jsonName" must "rename case class fields in both directions" in {
    val obj = Renamed("Ann", 3, plain = true)
    val json = mpjsons.serialize(obj)
    json mustBe """{"n":"Ann","a":3,"plain":true}"""
    mpjsons.deserialize[Renamed](json) mustBe obj
    mpjsons.deserialize[Renamed]("""{ n : "Bob", a : 4, plain : false }""") mustBe Renamed("Bob", 4, plain = false)
  }

  "@jsonName" must "still accept the Scala field name on read, so JSON written before the rename loads" in {
    mpjsons.deserialize[Renamed]("""{"name":"Ann","age":3,"plain":true}""") mustBe Renamed("Ann", 3, plain = true)
    mpjsons.deserialize[Renamed]("""{"n":"Ann","age":3,"plain":true}""") mustBe Renamed("Ann", 3, plain = true)
    mpjsons.deserialize[Renamed]("""{ name : "Ann", a : 3, plain : true }""") mustBe Renamed("Ann", 3, plain = true)
    mpjsons.deserialize[Bean]("""{"value":8,"other":"p"}""").value mustBe 8
    val derived = mpjsons.deserialize[Derived]("""{"identifier":6,"label":"m"}""")
    derived.identifier mustBe 6L
    derived.label mustBe "m"
    mpjsons.deserialize[Generic[Int]]("""{"value":1,"values":[2]}""") mustBe Generic(1, List(2))
    // when both names are present the last one wins, like any duplicate key
    mpjsons.deserialize[Renamed]("""{"name":"Old","n":"New","age":3,"plain":true}""") mustBe Renamed("New", 3, plain = true)
    // unrelated keys are still unknown
    intercept[JsonInnerException] { mpjsons.deserialize[Renamed]("""{"firstName":"Ann","age":3,"plain":true}""") }.getMessage must include("No field [firstName]")
  }

  "@jsonName" must "let a real JSON name win over another field's fallback name" in {
    // "a" is field b's JSON name, so it is not a fallback for field a; "b" likewise belongs to field a.
    mpjsons.deserialize[Swapped]("""{"b":1,"a":2}""") mustBe Swapped(1, 2)
    mpjsons.serialize(Swapped(1, 2)) mustBe """{"b":1,"a":2}"""
  }

  "@jsonName" must "work on vars, with the @field meta-annotation, and on inherited fields" in {
    val bean = new Bean
    bean.value = 7
    bean.other = "o"
    mpjsons.serialize(bean) mustBe """{"v":7,"other":"o"}"""
    mpjsons.deserialize[Bean]("""{"v":8,"other":"p"}""").value mustBe 8

    mpjsons.serialize(Meta("x")) mustBe """{"m":"x"}"""
    mpjsons.deserialize[Meta]("""{"m":"y"}""") mustBe Meta("y")

    val derived = new Derived
    derived.identifier = 5L
    derived.label = "lbl"
    mpjsons.serialize(derived) mustBe """{"id":5,"l":"lbl"}"""
    val back = mpjsons.deserialize[Derived]("""{"id":6,"l":"m"}""")
    back.identifier mustBe 6L
    back.label mustBe "m"
  }

  "@jsonName" must "reject duplicate JSON names at build time" in {
    val e = intercept[IllegalArgumentException] { mpjsons.buildStaticSerializer[Duplicate] }
    e.getMessage must include("x")
    e.getMessage must include("Duplicate")
    intercept[IllegalArgumentException] { mpjsons.buildStaticDeserializer[ClashWithPlain] }.getMessage must include("b")
  }

  "@jsonName" must "work together with generic fields" in {
    val obj = Generic("s", List("t"))
    mpjsons.serialize(obj) mustBe """{"v":"s","vs":["t"]}"""
    mpjsons.deserialize[Generic[String]](mpjsons.serialize(obj)) mustBe obj
    mpjsons.deserialize[Generic[Int]]("""{"v":1,"vs":[2]}""") mustBe Generic(1, List(2))
  }

  "@nullable" must "no longer require the @field meta-annotation on case class parameters" in {
    mpjsons.serialize(NullableNoMeta(null, "b")) mustBe """{"b":"b"}"""
    intercept[IllegalArgumentException] { mpjsons.serialize(NullableNoMeta("a", null)) }
  }
}
