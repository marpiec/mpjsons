package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object SkipSerializationSpec {
  case class Session(@skipSerialization token: String, user: String, @skipSerialization attempts: Int)

  class Bean {
    var visible: String = _
    @skipSerialization var cached: Long = 0
  }

  class Base { @skipSerialization var secret: String = _ }
  class Derived extends Base { var name: String = _ }

  case class Renamed(@skipSerialization @jsonName("t") token: String, user: String)

  // A type with no serializer of its own: serializing through it fails unless the field is skipped.
  trait Handler
  case class WithHandler(id: Int, @skipSerialization handler: Handler)

  case class WithOption(@skipSerialization note: Option[String], value: Option[Int])

  case class Validated(@skipSerialization note: String, n: Int) {
    require(n >= 0)
  }

  sealed trait Event
  case class Created(id: Int, @skipSerialization debug: String) extends Event
}

class SkipSerializationSpec extends AnyFlatSpec {
  import SkipSerializationSpec._

  val mpjsons = new MPJsons

  "@skipSerialization" must "leave case class fields out of the output" in {
    mpjsons.serialize(Session("abc", "ann", 3)) mustBe """{"user":"ann"}"""
  }

  it must "still read the field when the key is present" in {
    mpjsons.deserialize[Session]("""{"token":"abc","user":"ann","attempts":3}""") mustBe Session("abc", "ann", 3)
  }

  it must "give an absent field the value of any absent key" in {
    mpjsons.deserialize[Session]("""{"user":"ann"}""") mustBe Session(null, "ann", 0)
  }

  it must "work on vars and inherited fields" in {
    val bean = new Bean
    bean.visible = "v"
    bean.cached = 7L
    mpjsons.serialize(bean) mustBe """{"visible":"v"}"""
    mpjsons.deserialize[Bean]("""{"visible":"v","cached":7}""").cached mustBe 7L

    val derived = new Derived
    derived.secret = "s"
    derived.name = "n"
    mpjsons.serialize(derived) mustBe """{"name":"n"}"""
    mpjsons.deserialize[Derived]("""{"secret":"s","name":"n"}""").secret mustBe "s"
  }

  it must "keep reading the @jsonName key and the Scala name fallback" in {
    mpjsons.serialize(Renamed("abc", "ann")) mustBe """{"user":"ann"}"""
    mpjsons.deserialize[Renamed]("""{"t":"abc","user":"ann"}""") mustBe Renamed("abc", "ann")
    mpjsons.deserialize[Renamed]("""{"token":"abc","user":"ann"}""") mustBe Renamed("abc", "ann")
  }

  it must "not require a serializer for the field's type" in {
    mpjsons.serialize(WithHandler(1, new Handler {})) mustBe """{"id":1}"""
  }

  it must "not require a deserializer for the field's type while the key is absent" in {
    new MPJsons().deserialize[WithHandler]("""{"id":1}""") mustBe WithHandler(1, null)
  }

  it must "not apply the null policy to skipped fields" in {
    mpjsons.serialize(Session(null, "ann", 0)) mustBe """{"user":"ann"}"""
    mpjsons.serialize(WithOption(null, Some(1))) mustBe """{"value":{"value":1}}"""
  }

  it must "combine with the compact levers" in {
    val compact = new MPJsons(MPJsonsConfig(compactOptions = true, omitDefaults = true))
    compact.serialize(WithOption(Some("x"), Some(1))) mustBe """{"value":1}"""
    compact.serialize(WithOption(Some("x"), None)) mustBe "{}"
    compact.deserialize[WithOption]("""{"note":"x","value":1}""") mustBe WithOption(Some("x"), Some(1))
    compact.serialize(Session("abc", "ann", 3)) mustBe """{"user":"ann"}"""
  }

  it must "be read through the constructor in caseClassesViaConstructor mode" in {
    val viaConstructor = new MPJsons(MPJsonsConfig(caseClassesViaConstructor = true))
    viaConstructor.serialize(Validated("x", 1)) mustBe """{"n":1}"""
    viaConstructor.deserialize[Validated]("""{"note":"x","n":1}""") mustBe Validated("x", 1)
    viaConstructor.deserialize[Validated]("""{"n":1}""") mustBe Validated(null, 1)
  }

  it must "work inside typed wrappers" in {
    val typed = new MPJsons
    typed.markTypedClass[Event]()
    typed.serialize[List[Event]](List(Created(1, "dbg"))) mustBe """[{"Created":{"id":1}}]"""
    typed.deserialize[List[Event]]("""[{"Created":{"id":1,"debug":"dbg"}}]""") mustBe List(Created(1, "dbg"))
  }
}
