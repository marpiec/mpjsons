package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object RecursionLimitsSpec {
  class Node {
    var name: String = _
    @nullable var next: Node = _
    var children: List[Node] = Nil
  }
  case class Pair(left: Node, right: Node)
  case class Deep(@nullable child: Deep)
  case class Tree(children: List[Tree])

  def chain(length: Int): Node = {
    val head = new Node
    head.name = "0"
    var current = head
    for (i <- 1 until length) {
      val next = new Node
      next.name = i.toString
      current.next = next
      current = next
    }
    head
  }

  def nestedObjects(levels: Int): String = """{"child":""" * levels + "{}" + "}" * levels
}

class RecursionLimitsSpec extends AnyFlatSpec {
  import RecursionLimitsSpec._

  val mpjsons = new MPJsons

  // ---------------------------------------------------------------- cycles on write

  "Serializer" must "report a reference cycle instead of overflowing the stack" in {
    val self = new Node
    self.name = "self"
    self.next = self
    val e = intercept[IllegalArgumentException] { mpjsons.serialize(self) }
    e.getMessage must include("Cyclic reference")
    e.getMessage must include("Node")

    val a = chain(3)
    a.next.next.next = a // a -> b -> c -> a
    intercept[IllegalArgumentException] { mpjsons.serialize(a) }.getMessage must include("Cyclic reference")

    val parent = new Node
    parent.name = "parent"
    val child = new Node
    child.name = "child"
    child.children = List(parent) // cycle through a collection
    parent.children = List(child)
    intercept[IllegalArgumentException] { mpjsons.buildStaticSerializer[Node].serialize(parent) }.getMessage must include("Cyclic reference")
  }

  "Serializer" must "not mistake shared references or deep acyclic graphs for cycles" in {
    val shared = chain(1)
    mpjsons.serialize(Pair(shared, shared)) mustBe """{"left":{"name":"0","children":[]},"right":{"name":"0","children":[]}}"""

    val long = chain(1000)
    mpjsons.deserialize[Node](mpjsons.serialize(long)).next.next.name mustBe "2"
  }

  "Serializer" must "leave no state behind after a cycle error" in {
    val self = new Node
    self.name = "self"
    self.next = self
    intercept[IllegalArgumentException] { mpjsons.serialize(self) }
    mpjsons.serialize(chain(2)) mustBe """{"name":"0","next":{"name":"1","children":[]},"children":[]}"""
    mpjsons.serialize(chain(300)).length must be > 0
  }

  // ---------------------------------------------------------------- depth on read

  "maxDepth" must "reject input nested deeper than the limit, including skipped fields" in {
    val limited = new MPJsons(MPJsonsConfig(maxDepth = 50))
    limited.deserialize[Deep](nestedObjects(40)) must not be null

    val e = intercept[JsonInnerException] { limited.deserialize[Deep](nestedObjects(60)) }
    e.getMessage must include("maxDepth")
    intercept[JsonInnerException] { limited.deserialize[Deep](nestedObjects(100000)) }.getMessage must include("maxDepth")
    intercept[JsonInnerException] { limited.deserialize[Tree]("""{"children":""" + "[{\"children\":" * 30 + "[]" + "}]" * 30 + "}") }.getMessage must include("maxDepth")
    intercept[JsonInnerException] { limited.buildStaticDeserializer[Deep].deserialize(nestedObjects(60)) }.getMessage must include("maxDepth")

    val lenientLimited = new MPJsons(MPJsonsConfig(maxDepth = 50, ignoreNonExistingFields = true))
    val hostile = """{"unknown":""" + "[" * 100000 + "]" * 100000 + ""","child":null}"""
    intercept[JsonInnerException] { lenientLimited.deserialize[Deep](hostile) }.getMessage must include("maxDepth")
    lenientLimited.deserialize[Deep]("""{"unknown":[[[1]]],"child":{}}""") mustBe Deep(Deep(null))
  }

  "maxDepth" must "leave the default unlimited and keep fast paths" in {
    mpjsons.config.maxDepth mustBe 0
    mpjsons.deserialize[Deep](nestedObjects(1000)) must not be null
    val limited = new MPJsons(MPJsonsConfig(maxDepth = 10))
    limited.deserialize[List[Int]]("[1,2,3]") mustBe List(1, 2, 3)
    limited.deserialize[Map[String, List[Int]]]("""[["a",[1]]]""") mustBe Map("a" -> List(1))
  }
}
