package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object SingletonObjectSpec {
  object Marker
  case object Flag
  case class Holder(m: Marker.type, f: Flag.type, n: Int)
}

class SingletonObjectSpec extends AnyFlatSpec {
  import SingletonObjectSpec._

  val mpjsons = new MPJsons

  "Serializer" must "serialize singleton objects as empty objects" in {
    mpjsons.serialize(Marker) mustBe "{}"
    mpjsons.serialize(Flag) mustBe "{}"
    mpjsons.serialize(Holder(Marker, Flag, 1)) mustBe """{"m":{},"f":{},"n":1}"""
  }

  "Deserializer" must "return the singleton instance for fields of singleton type" in {
    val holder = mpjsons.deserialize[Holder]("""{"m":{},"f":{},"n":1}""")
    holder.n mustBe 1
    holder.m must be theSameInstanceAs Marker
    holder.f must be theSameInstanceAs Flag
  }

  "Deserializer" must "return the singleton instance at top level" in {
    mpjsons.deserialize[Marker.type]("{}") must be theSameInstanceAs Marker
    mpjsons.deserialize[Flag.type](" { } ") must be theSameInstanceAs Flag
  }

  "Deserializer" must "reject non-empty objects for singleton types" in {
    intercept[io.mpjsons.impl.JsonInnerException] {
      mpjsons.deserialize[Marker.type]("""{"a":1}""")
    }
  }
}
