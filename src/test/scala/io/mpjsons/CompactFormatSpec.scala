package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object CompactFormatSpec {
  case class Url(path: String)
}

/** Opt-in format levers of MPJsonsConfig; defaults keep the historical format. */
class CompactFormatSpec extends AnyFlatSpec {
  import CompactFormatSpec._

  val legacy = new MPJsons
  val compact = new MPJsons(MPJsonsConfig(escapeSlash = false))

  "MPJsons" must "still accept the historical constructor arguments" in {
    new MPJsons(true).config.ignoreNonExistingFields mustBe true
    new MPJsons(ignoreNullFields = true).config.nullPolicy mustBe NullPolicy.Omit
    new MPJsons(nullPolicy = NullPolicy.WriteNull).config.nullPolicy mustBe NullPolicy.WriteNull
    legacy.config mustBe MPJsonsConfig()
  }

  "escapeSlash" must "control whether / is escaped and never affect reading" in {
    legacy.serialize(Url("a/b")) mustBe """{"path":"a\/b"}"""
    compact.serialize(Url("a/b")) mustBe """{"path":"a/b"}"""
    legacy.deserialize[Url]("""{"path":"a\/b"}""") mustBe Url("a/b")
    legacy.deserialize[Url]("""{"path":"a/b"}""") mustBe Url("a/b")
    compact.deserialize[Url]("""{"path":"a\/b"}""") mustBe Url("a/b")
    compact.serialize("x/y\"z") mustBe "\"x/y\\\"z\""
  }
}
