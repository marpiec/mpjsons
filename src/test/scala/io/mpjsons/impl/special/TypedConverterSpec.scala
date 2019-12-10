package io.mpjsons.impl.special


import io.mpjsons.MPJsons
import org.scalatest.{FlatSpec, MustMatchers}

sealed trait CustomDescription

case class TextDescription(text: String) extends CustomDescription
case class NumericDescription(number: Int) extends CustomDescription


class TypedConverterSpec extends FlatSpec with MustMatchers {

  "Typed serializer" must "generate correct JSON" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[CustomDescription]

    val serialized: String = mpjsons.serialize(List[CustomDescription](TextDescription("Hello"), NumericDescription(14)))

    serialized mustBe """[{"TextDescription":{"text":"Hello"}},{"NumericDescription":{"number":14}}]"""
  }

  "Typed deserializer" must "correctly parse JSON" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[CustomDescription]

    val serialized = """[{"TextDescription":{"text":"Hello"}},{"NumericDescription":{"number":14}}]"""

    val deserialized = mpjsons.deserialize[List[CustomDescription]](serialized)

    deserialized mustBe List(TextDescription("Hello"), NumericDescription(14))

  }
}

