package io.mpjsons

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.values.StringDeserializer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object LateRegistrationSpec {
  case class Money(cents: Long)
  case class Wallet(owner: String, balance: Money)
  case class Sample(@nullable a: String, @nullable b: String)

  sealed trait Pet
  case class Cat(name: String) extends Pet
}

/** Registrations made after a type was first (de)serialized must still take effect. */
class LateRegistrationSpec extends AnyFlatSpec {
  import LateRegistrationSpec._

  private def registerMoneyAsString(mpjsons: MPJsons): Unit = {
    mpjsons.registerConverter[Money](
      sf => (obj: Money, jsonBuilder: StringBuilder) => { jsonBuilder.append('"').append(obj.cents).append("c\"") },
      df => (jsonIterator: StringIterator) => Money(StringDeserializer.deserialize(jsonIterator).dropRight(1).toLong))
  }

  "registerConverter" must "apply to types and enclosing types already serialized and deserialized" in {
    val mpjsons = new MPJsons
    mpjsons.serialize(Money(5)) mustBe """{"cents":5}"""
    mpjsons.serialize(Wallet("Ann", Money(5))) mustBe """{"owner":"Ann","balance":{"cents":5}}"""
    mpjsons.deserialize[Wallet]("""{"owner":"Ann","balance":{"cents":5}}""") mustBe Wallet("Ann", Money(5))

    registerMoneyAsString(mpjsons)

    mpjsons.serialize(Money(7)) mustBe "\"7c\""
    mpjsons.serialize(Wallet("Bo", Money(8))) mustBe """{"owner":"Bo","balance":"8c"}"""
    mpjsons.deserialize[Wallet]("""{"owner":"Bo","balance":"9c"}""") mustBe Wallet("Bo", Money(9))
    mpjsons.serialize(List(Money(1))) mustBe """["1c"]"""
  }

  "postDeserializationTransform" must "apply after the type was already deserialized" in {
    val mpjsons = new MPJsons
    mpjsons.deserialize[Sample]("""{"a":"x"}""") mustBe Sample("x", null)
    mpjsons.postDeserializationTransform[Sample](s => if (s.b == null) s.copy(b = "") else s)
    mpjsons.deserialize[Sample]("""{"a":"x"}""") mustBe Sample("x", "")
  }

  "markTypedClass" must "apply after the hierarchy was already used" in {
    val mpjsons = new MPJsons
    mpjsons.serialize[Pet](Cat("Tom")) mustBe "{}" // the trait itself has no fields
    mpjsons.markTypedClass[Pet]
    mpjsons.serialize[Pet](Cat("Tom")) mustBe """{"Cat":{"name":"Tom"}}"""
    mpjsons.deserialize[Pet]("""{"Cat":{"name":"Tom"}}""") mustBe Cat("Tom")
  }

  "static serializers" must "keep the behaviour they were built with" in {
    val mpjsons = new MPJsons
    val before = mpjsons.buildStaticSerializer[Money]
    registerMoneyAsString(mpjsons)
    before.serialize(Money(3)) mustBe """{"cents":3}"""
    mpjsons.buildStaticSerializer[Money].serialize(Money(3)) mustBe "\"3c\""
  }
}
