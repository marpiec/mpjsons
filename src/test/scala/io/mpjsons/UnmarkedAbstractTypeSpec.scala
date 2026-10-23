package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object UnmarkedAbstractTypeSpec {
  trait Pet
  case class Cat(name: String) extends Pet
  case class Home(pet: Pet)

  abstract class Account { var id: Long = 0 }
  case class Savings(rate: Double) extends Account
  case class Bank(account: Account)
}

/** Serializing through a trait or abstract class without a converter would lose the concrete class, so it fails. */
class UnmarkedAbstractTypeSpec extends AnyFlatSpec {
  import UnmarkedAbstractTypeSpec._

  "Serializer" must "reject a value whose static type is an unmarked trait" in {
    val mpjsons = new MPJsons
    val e = intercept[IllegalArgumentException] { mpjsons.serialize[Pet](Cat("Tom")) }
    e.getMessage must include("Cannot serialize abstract type")
    e.getMessage must include("Pet")
    e.getMessage must include("markTypedClass")

    intercept[IllegalArgumentException] { mpjsons.serialize(Home(Cat("Tom"))) }.getMessage must include("Pet")
    intercept[IllegalArgumentException] { mpjsons.serialize(List[Pet](Cat("Tom"))) }.getMessage must include("Pet")
    intercept[IllegalArgumentException] { mpjsons.serialize(Option[Pet](Cat("Tom"))) }.getMessage must include("Pet")
    intercept[IllegalArgumentException] { mpjsons.buildStaticSerializer[Pet] }.getMessage must include("Pet")
  }

  "Serializer" must "reject an unmarked abstract class, even one with fields" in {
    val mpjsons = new MPJsons
    val savings = Savings(1.5)
    savings.id = 7
    intercept[IllegalArgumentException] { mpjsons.serialize(Bank(savings)) }.getMessage must include("Account")
    mpjsons.serialize(savings) mustBe """{"id":7,"rate":1.5}"""
  }

  "Serializer" must "still serialize concrete classes and marked or converted abstract types" in {
    val mpjsons = new MPJsons
    mpjsons.serialize(Cat("Tom")) mustBe """{"name":"Tom"}"""

    mpjsons.markTypedClass[Pet]
    mpjsons.serialize(Home(Cat("Tom"))) mustBe """{"pet":{"Cat":{"name":"Tom"}}}"""
    mpjsons.deserialize[Home]("""{"pet":{"Cat":{"name":"Tom"}}}""") mustBe Home(Cat("Tom"))

    val converted = new MPJsons
    converted.registerSuperclassConverter[Account](
      sf => (obj: Account, jsonBuilder: StringBuilder) => { jsonBuilder.append(obj.id) },
      df => (jsonIterator: impl.StringIterator) => { val s = Savings(0); s.id = impl.deserializer.values.LongDeserializer.deserialize(jsonIterator); s })
    val savings = Savings(1.5)
    savings.id = 9
    converted.serialize(Bank(savings)) mustBe """{"account":9}"""
  }
}
