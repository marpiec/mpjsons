package io.mpjsons

import org.scalatest.FlatSpec
import org.scalatest.MustMatchers._

object AbstractSerializationSpec {

  trait CommonType

  case class A(a: String) extends CommonType
  case class B(b: Int) extends CommonType

}


class AbstractSerializationSpec extends FlatSpec {

  import AbstractSerializationSpec._

  val mpjsons = new MPJsons

  mpjsons.markTypedClass[CommonType]

  "Serializer" must "add type information while serializing trait" in {

    val a = A("Hello")
    val b = B(42)

    val aCommon: CommonType = a
    val bCommon: CommonType = b

    mpjsons.serialize(a) mustEqual """{"a":"Hello"}"""
    mpjsons.serialize(b) mustEqual """{"b":42}"""
    mpjsons.serialize(aCommon) mustEqual """{"A":{"a":"Hello"}}"""
    mpjsons.serialize(bCommon) mustEqual """{"B":{"b":42}}"""

  }

}
