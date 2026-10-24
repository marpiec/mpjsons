package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object TypedNameMatchingSpec {
  sealed trait Event
  case class A(v: Int) extends Event
  case class AB(v: Int) extends Event
  case class ABC(v: Int) extends Event
  @typeName("q\"x") case class Quoted(v: Int) extends Event
  @typeName("b\\s") case class Backslash(v: Int) extends Event
}

/** Type names already read are matched in place on later reads; results must not depend on what was read before. */
class TypedNameMatchingSpec extends AnyFlatSpec {
  import TypedNameMatchingSpec._

  private def typed(): MPJsons = {
    val m = new MPJsons
    m.markTypedClass[Event]
    m
  }

  "Typed values" must "read the same whichever names were read before" in {
    val inputs = List(
      """{"ABC":{"v":3}}""" -> ABC(3),
      """{"A":{"v":1}}""" -> A(1),
      """{"AB":{"v":2}}""" -> AB(2),
      """{ "AB" : {"v": 4} }""" -> AB(4),
      """{"AB":{"v":5}}""" -> AB(5),
      """{"ABC":{"v":6}}""" -> ABC(6),
      """{"q\"x":{"v":7}}""" -> Quoted(7),
      """{"b\\s":{"v":8}}""" -> Backslash(8))
    // every order of first reads, then everything again with all names known
    for (order <- inputs.permutations.take(40)) {
      val m = typed()
      val static = m.buildStaticDeserializer[Event]
      for ((json, expected) <- order ++ order) {
        m.deserialize[Event](json) mustBe expected
        static.deserialize(json) mustBe expected
        m.deserialize[List[Event]]("[" + json + "," + json + "]") mustBe List(expected, expected)
      }
    }
  }

  it must "fail on unknown and prefix names after known ones like before" in {
    val m = typed()
    m.deserialize[Event]("""{"AB":{"v":1}}""") mustBe AB(1)
    val unknown = the[JsonInnerException] thrownBy m.deserialize[Event]("""{"ABCD":{"v":1}}""")
    unknown.getMessage must include("Unknown type name 'ABCD'")
    val truncated = the[JsonInnerException] thrownBy m.deserialize[Event]("""{"AB""")
    truncated.getMessage must include("Unexpected end of Json")
  }

  it must "read type names through a String transform registered after names were read" in {
    val m = typed()
    m.deserialize[Event]("""{"A":{"v":1}}""") mustBe A(1)
    m.deserialize[Event]("""{"AB":{"v":1}}""") mustBe AB(1)
    // reads "A" as "AB": the type name is read as a String, so the transform applies to it
    m.postDeserializationTransform[String](s => if (s == "A") "AB" else s)
    m.deserialize[Event]("""{"A":{"v":2}}""") mustBe AB(2)
    m.deserialize[Event]("""{"AB":{"v":3}}""") mustBe AB(3)
  }

  it must "read type names like 0.7.2 with a String transform" in {
    val current = typed()
    val legacy = new io.mpjsons.legacy.MPJsons
    legacy.markTypedClass[Event]
    current.postDeserializationTransform[String](s => if (s == "A") "AB" else s)
    legacy.postDeserializationTransform[String](s => if (s == "A") "AB" else s)
    for (json <- List("""{"A":{"v":2}}""", """{"AB":{"v":3}}""", """{"A":{"v":4}}""")) {
      current.deserialize[Event](json) mustBe legacy.deserialize[Event](json)
    }
  }
}
