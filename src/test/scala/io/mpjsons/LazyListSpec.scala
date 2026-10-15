package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object LazyListSpec {
  case class Holder(names: LazyList[String], numbers: LazyList[Int], nested: LazyList[LazyList[Int]])
}

class LazyListSpec extends AnyFlatSpec {
  import LazyListSpec._

  val mpjsons = new MPJsons

  "Serializer" must "round trip LazyList like any sequence" in {
    val holder = Holder(LazyList("a", "b"), LazyList(1, 2, 3), LazyList(LazyList(1), LazyList.empty))
    val json = mpjsons.serialize(holder)
    json mustBe """{"names":["a","b"],"numbers":[1,2,3],"nested":[[1],[]]}"""

    val back = mpjsons.deserialize[Holder](json)
    back.names.toList mustBe List("a", "b")
    back.numbers.toList mustBe List(1, 2, 3)
    back.nested.map(_.toList).toList mustBe List(List(1), Nil)
    back mustBe holder
  }

  "Deserializer" must "handle top-level and empty LazyLists" in {
    mpjsons.deserialize[LazyList[Int]]("[4, 5]").toList mustBe List(4, 5)
    mpjsons.deserialize[LazyList[Int]]("[]") mustBe LazyList.empty
    mpjsons.serialize(LazyList.from(1).take(3)) mustBe "[1,2,3]"
  }
}
