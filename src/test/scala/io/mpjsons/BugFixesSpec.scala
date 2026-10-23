package io.mpjsons

import java.util.concurrent.{Callable, CountDownLatch, Executors, TimeUnit}

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

object BugFixesSpec {

  case class Simple(a: Boolean, b: Int)

  case class WithList(values: List[Int], name: String)

  class EmptyBean

  case class Box[T](value: T, values: List[T], option: Option[T])

  case class Item(name: String)

  case class Boxes(strings: Box[String], items: Box[Item])

  case class Nested(children: List[Nested])

  case class Many1(a: Int, b: String, c: List[Long])
  case class Many2(x: Double, y: Option[Many1], z: Map[String, Many1])
  case class Many3(m: Vector[Many2], n: Set[String], o: Array[Int])
}

class BugFixesSpec extends AnyFlatSpec with Matchers {

  import BugFixesSpec._

  "Deserializer" must "accept empty object containing whitespace" in {
    new MPJsons().deserialize[EmptyBean]("{ }") mustBe an[EmptyBean]
    new MPJsons().deserialize[EmptyBean]("{\n\t}") mustBe an[EmptyBean]
    new MPJsons().deserialize[Option[Int]]("{ }") mustBe None
  }

  it must "accept trailing comma followed by whitespace, like it accepts trailing comma without whitespace" in {
    val mpjsons = new MPJsons()
    mpjsons.deserialize[Simple]("""{"a":true,"b":3,}""") mustBe Simple(a = true, 3)
    mpjsons.deserialize[Simple]("""{"a":true,"b":3, }""") mustBe Simple(a = true, 3)
    mpjsons.deserialize[WithList]("""{"values":[1,2,],"name":"x"}""") mustBe WithList(List(1, 2), "x")
    mpjsons.deserialize[WithList]("""{"values":[1,2, ],"name":"x"}""") mustBe WithList(List(1, 2), "x")
    mpjsons.deserialize[Map[String, Int]]("""[["a",1], ]""") mustBe Map("a" -> 1)
    mpjsons.deserialize[Array[Long]]("""[1, 2,  ]""") mustBe Array(1L, 2L)
  }

  it must "not lose fields following skipped empty object with whitespace" in {
    val mpjsons = new MPJsons(ignoreNonExistingFields = true)
    mpjsons.deserialize[Simple]("""{"unknown": { }, "b": 3, "a": true}""") mustBe Simple(a = true, 3)
    mpjsons.deserialize[Simple]("""{"unknown": [1, { } ], "b": 3, "a": true}""") mustBe Simple(a = true, 3)
    mpjsons.deserialize[Simple]("""{"unknown": [1, 2, ], "b": 3, "a": true}""") mustBe Simple(a = true, 3)
  }

  it must "use proper type arguments for the same generic type used with different arguments" in {
    val mpjsons = new MPJsons()
    val boxes = Boxes(Box("a", List("b", "c"), Some("d")), Box(Item("e"), List(Item("f")), Some(Item("g"))))
    val json = """{"strings":{"value":"a","values":["b","c"],"option":{"value":"d"}},""" +
      """"items":{"value":{"name":"e"},"values":[{"name":"f"}],"option":{"value":{"name":"g"}}}}"""
    mpjsons.deserialize[Boxes](json) mustBe boxes
  }

  it must "report too deeply nested json as JsonInnerException instead of StackOverflowError" in {
    val depth = 200000
    val deepArray = "[" * depth + "]" * depth
    an[JsonInnerException] must be thrownBy new MPJsons(ignoreNonExistingFields = true).deserialize[Simple]("""{"x":""" + deepArray + ""","a":true,"b":1}""")

    val deepTree = """{"children":[""" * depth + "]}" * depth
    an[JsonInnerException] must be thrownBy new MPJsons().deserialize[Nested](deepTree)
    an[JsonInnerException] must be thrownBy new MPJsons().buildStaticDeserializer[Nested].deserialize(deepTree)
  }

  it must "keep error message small for big json" in {
    val json = """{"a":true,"marker":"""" + "x" * 100000 + """","b":"not a number"}"""
    val message = intercept[JsonInnerException](new MPJsons(ignoreNonExistingFields = true).deserialize[Simple](json)).getMessage
    message.length must be < 5000
  }

  it must "be safe to use concurrently from many threads" in {
    val json3 = """{"m":[{"x":1.5,"y":{"value":{"a":1,"b":"q","c":[1,2]}},"z":[["k",{"a":2,"b":"r","c":[]}]]}],"n":["s"],"o":[1,2,3]}"""
    val threads = 16
    for (_ <- 0 until 20) {
      val mpjsons = new MPJsons()
      val pool = Executors.newFixedThreadPool(threads)
      val start = new CountDownLatch(1)
      val futures = (0 until threads).map { i =>
        pool.submit(new Callable[String] {
          override def call(): String = {
            start.await()
            (i % 3) match {
              case 0 => mpjsons.deserialize[Many3](json3).m.head.z("k").b
              case 1 => mpjsons.deserialize[Many1]("""{"a":1,"b":"q","c":[1,2]}""").b
              case _ => mpjsons.deserialize[Many2]("""{"x":1.5,"z":[["k",{"a":2,"b":"r","c":[]}]]}""").z("k").b
            }
          }
        })
      }
      start.countDown()
      futures.map(_.get(30, TimeUnit.SECONDS)).toSet mustBe Set("r", "q")
      pool.shutdown()
    }
  }

  it must "reject a call without a type argument" in {
    the[IllegalArgumentException] thrownBy new MPJsons().deserialize("{}") must have message
      "Type for object deserialization was not specified, or was Nothing. Please specify object type."
  }

  "Serializer" must "serialize tuples with elements of different types" in {
    val mpjsons = new MPJsons()
    val value: (Item, Int) = (Item("a"), 5)
    val json = mpjsons.serialize(value)
    mpjsons.deserialize[(Item, Int)](json) mustBe value
    val nested: (Item, (Long, Double)) = (Item("b"), (1L, 2.5))
    mpjsons.deserialize[(Item, (Long, Double))](mpjsons.serialize(nested)) mustBe nested
  }
}
