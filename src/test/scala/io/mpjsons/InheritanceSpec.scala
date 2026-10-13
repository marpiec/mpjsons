package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object InheritanceSpec {

  class Base {
    var baseField: Int = 1
  }

  class Derived extends Base {
    var derivedField: Int = 2
  }

  class GenericBase[T] {
    var payload: T = _
    var tags: List[T] = _
  }

  class StringDerived extends GenericBase[String] {
    var count: Int = 0
  }

  abstract class Entity {
    var id: Long = 0
  }

  case class User(name: String) extends Entity

  class Level1 { var a: Int = 0 }
  class Level2 extends Level1 { var b: Int = 0 }
  class Level3 extends Level2 { var c: Int = 0 }
}

class InheritanceSpec extends AnyFlatSpec {
  import InheritanceSpec._

  val mpjsons = new MPJsons

  "Serializer" must "include fields declared in superclasses" in {
    val derived = new Derived
    derived.baseField = 10
    derived.derivedField = 20

    val json = mpjsons.serialize(derived)
    json mustBe """{"derivedField":20,"baseField":10}"""

    val deserialized = mpjsons.deserialize[Derived](json)
    deserialized.baseField mustBe 10
    deserialized.derivedField mustBe 20
  }

  "Serializer" must "resolve generic superclass fields as seen from the concrete type" in {
    val obj = new StringDerived
    obj.payload = "hello"
    obj.tags = List("x", "y")
    obj.count = 3

    val json = mpjsons.serialize(obj)
    json mustBe """{"count":3,"tags":["x","y"],"payload":"hello"}"""

    val deserialized = mpjsons.deserialize[StringDerived](json)
    deserialized.payload mustBe "hello"
    deserialized.tags mustBe List("x", "y")
    deserialized.count mustBe 3
  }

  "Serializer" must "handle case classes extending classes with fields" in {
    val user = User("Ann")
    user.id = 42L

    val json = mpjsons.serialize(user)
    json mustBe """{"name":"Ann","id":42}"""

    val deserialized = mpjsons.deserialize[User](json)
    deserialized mustBe user
    deserialized.id mustBe 42L
  }

  "Serializer" must "walk the whole class hierarchy" in {
    val obj = new Level3
    obj.a = 1; obj.b = 2; obj.c = 3

    val json = mpjsons.serialize(obj)
    json mustBe """{"c":3,"b":2,"a":1}"""

    val deserialized = mpjsons.deserialize[Level3](json)
    (deserialized.a, deserialized.b, deserialized.c) mustBe ((1, 2, 3))
  }
}
