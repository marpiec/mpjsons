package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object JdkSuperclassSpec {
  case class DomainError(code: Int, detail: String) extends Exception("code " + code)
  class Counter extends java.util.concurrent.atomic.AtomicInteger(5) {
    var label: String = _
  }
}

/** Fields of JDK superclasses are neither accessible under the module system nor part of the format: they are skipped. */
class JdkSuperclassSpec extends AnyFlatSpec {
  import JdkSuperclassSpec._

  val mpjsons = new MPJsons

  "Serializer" must "serialize only the Scala fields of a class extending a JDK class" in {
    mpjsons.serialize(DomainError(7, "d")) mustBe """{"code":7,"detail":"d"}"""
    val back = mpjsons.deserialize[DomainError]("""{"code":7,"detail":"d"}""")
    back.code mustBe 7
    back.detail mustBe "d"

    val counter = new Counter
    counter.label = "l"
    mpjsons.serialize(counter) mustBe """{"label":"l"}"""
    mpjsons.deserialize[Counter]("""{"label":"m"}""").label mustBe "m"
  }
}
