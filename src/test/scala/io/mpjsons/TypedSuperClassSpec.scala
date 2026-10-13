package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object TypedSuperClassSpec {
  trait Animal
  trait Pet extends Animal
  case class Dog(name: String) extends Pet
  case class Wolf(name: String) extends Animal
  case class Kennel(pet: Pet, animals: List[Animal], pets: List[Pet])

  sealed trait Shape
  sealed trait Round extends Shape
  case class Circle(r: Double) extends Round
  case class Square(side: Int) extends Shape
  case class Holder(round: Round, rounds: List[Round], shapes: List[Shape])
}

/**
 * With markTypedSuperClass the typed wrapper is also used for subtypes of the marked type; a value read through
 * such a subtype must be an instance of that subtype, not just of the marked type.
 */
class TypedSuperClassSpec extends AnyFlatSpec {
  import TypedSuperClassSpec._

  "markTypedSuperClass" must "only accept subtypes of the declared type being read (non-sealed hierarchy)" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedSuperClass[Animal]

    val kennel = Kennel(Dog("Rex"), List(Dog("Rex"), Wolf("Grey")), List(Dog("Fido")))
    val json = mpjsons.serialize(kennel)
    json mustBe """{"pet":{"Dog":{"name":"Rex"}},"animals":[{"Dog":{"name":"Rex"}},{"Wolf":{"name":"Grey"}}],"pets":[{"Dog":{"name":"Fido"}}]}"""
    mpjsons.deserialize[Kennel](json) mustBe kennel

    val e = intercept[JsonInnerException] { mpjsons.deserialize[List[Pet]]("""[{"Wolf":{"name":"Grey"}}]""") }
    e.getMessage must include("not a subtype")
    e.getMessage must include("Pet")
    intercept[JsonInnerException] { mpjsons.deserialize[Kennel]("""{"pet":{"Wolf":{"name":"Grey"}},"animals":[],"pets":[]}""") }.getMessage must include("not a subtype")
    intercept[JsonInnerException] { mpjsons.deserialize[Dog]("""{"Wolf":{"name":"Grey"}}""") }.getMessage must include("not a subtype")
  }

  "markTypedSuperClass" must "only accept subtypes of the declared type being read (sealed hierarchy)" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedSuperClass[Shape]

    val holder = Holder(Circle(1.0), List(Circle(2.0)), List(Circle(3.0), Square(4)))
    mpjsons.deserialize[Holder](mpjsons.serialize(holder)) mustBe holder

    val e = intercept[JsonInnerException] { mpjsons.deserialize[List[Round]]("""[{"Square":{"side":4}}]""") }
    e.getMessage must include("not a subtype")
    e.getMessage must include("Round")
    mpjsons.deserialize[List[Shape]]("""[{"Square":{"side":4}},{"Circle":{"r":1.0}}]""") mustBe List(Square(4), Circle(1.0))
  }
}
