package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object GenericTypeParamsSpec {
  case class Box[T](items: List[T], single: T, nested: Option[List[T]])
  case class Pair[A, B](a: A, bs: List[B])
  case class Outer(ints: Box[Int], strings: Box[String])
}

class GenericTypeParamsSpec extends AnyFlatSpec {
  import GenericTypeParamsSpec._

  "Deserializer" must "not reuse type parameter bindings between instantiations of a generic type" in {
    val mpjsons = new MPJsons

    val ints = mpjsons.deserialize[Box[Int]]("""{"items":[1,2],"single":3,"nested":{"value":[4]}}""")
    ints mustBe Box(List(1, 2), 3, Some(List(4)))

    val strings = mpjsons.deserialize[Box[String]]("""{"items":["a","b"],"single":"c","nested":{"value":["d"]}}""")
    strings mustBe Box(List("a", "b"), "c", Some(List("d")))
  }

  "Serializer" must "not reuse type parameter bindings between instantiations of a generic type" in {
    val mpjsons = new MPJsons

    mpjsons.serialize(Box(List(1), 2, Some(List(3)))) mustBe """{"items":[1],"single":2,"nested":{"value":[3]}}"""
    mpjsons.serialize(Box(List("x"), "y", Some(List("z")))) mustBe """{"items":["x"],"single":"y","nested":{"value":["z"]}}"""
  }

  "Serializer" must "handle two instantiations of the same generic type inside one object" in {
    val mpjsons = new MPJsons

    val outer = Outer(Box(List(1), 2, None), Box(List("a"), "b", None))
    val json = mpjsons.serialize(outer)
    json mustBe """{"ints":{"items":[1],"single":2,"nested":{}},"strings":{"items":["a"],"single":"b","nested":{}}}"""
    mpjsons.deserialize[Outer](json) mustBe outer
  }

  "Serializer" must "handle multiple type parameters" in {
    val mpjsons = new MPJsons

    val p1 = Pair[Int, String](1, List("a"))
    val p2 = Pair[String, Int]("x", List(2))
    mpjsons.deserialize[Pair[Int, String]](mpjsons.serialize(p1)) mustBe p1
    mpjsons.deserialize[Pair[String, Int]](mpjsons.serialize(p2)) mustBe p2
  }
}
