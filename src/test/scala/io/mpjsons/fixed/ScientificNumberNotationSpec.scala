package io.mpjsons.fixed

import io.mpjsons.MPJsons
import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

object ScientificNumberNotationSpec {
  case class WithNumber(a: Int, b: Double, c: Double)
}

class ScientificNumberNotationSpec extends FlatSpec {
  import ScientificNumberNotationSpec._

  private val mpjsons = new MPJsons

  private val properJson =
    """
      |{"a":30,"b":1.2e4,"c":2.1e2}
    """.stripMargin

  "Deserializer" must "Handle scientific notation" in {
    val deserialized = mpjsons.deserialize[WithNumber](properJson)

    deserialized.a mustBe 30
    deserialized.b mustBe 12000
    deserialized.c mustBe 210
  }

}
