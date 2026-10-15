package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import scala.collection.mutable

object MutableCollectionsSpec {

  class MutableHolder {
    var arrayBuffer: mutable.ArrayBuffer[Int] = _
    var buffer: mutable.Buffer[String] = _
    var seq: mutable.Seq[Int] = _
    var indexedSeq: mutable.IndexedSeq[Int] = _
    var iterable: mutable.Iterable[Int] = _
    var set: mutable.Set[String] = _
    var hashSet: mutable.HashSet[Int] = _
    var map: mutable.Map[Int, String] = _
    var hashMap: mutable.HashMap[String, Int] = _
  }

  trait Shape
  case class ShapeHolder(shape: Shape)

  abstract class Animal { var name: String = _ }
  case class AnimalHolder(animal: Animal)
}

class MutableCollectionsSpec extends AnyFlatSpec {
  import MutableCollectionsSpec._

  val mpjsons = new MPJsons

  "Serializer" must "round trip mutable collections" in {
    val holder = new MutableHolder
    holder.arrayBuffer = mutable.ArrayBuffer(1, 2, 3)
    holder.buffer = mutable.Buffer("a", "b")
    holder.seq = mutable.Seq(4, 5)
    holder.indexedSeq = mutable.IndexedSeq(6)
    holder.iterable = mutable.Iterable(7, 8)
    holder.set = mutable.Set("x", "y")
    holder.hashSet = mutable.HashSet(9, 10)
    holder.map = mutable.Map(1 -> "one", 2 -> "two")
    holder.hashMap = mutable.HashMap("three" -> 3)

    val json = mpjsons.serialize(holder)
    val deserialized = mpjsons.deserialize[MutableHolder](json)

    deserialized.arrayBuffer mustBe holder.arrayBuffer
    deserialized.buffer mustBe holder.buffer
    deserialized.seq mustBe holder.seq
    deserialized.indexedSeq mustBe holder.indexedSeq
    deserialized.iterable.toList mustBe holder.iterable.toList
    deserialized.set mustBe holder.set
    deserialized.hashSet mustBe holder.hashSet
    deserialized.map mustBe holder.map
    deserialized.hashMap mustBe holder.hashMap

    deserialized.arrayBuffer += 4
    deserialized.arrayBuffer mustBe mutable.ArrayBuffer(1, 2, 3, 4)
  }

  "Deserializer" must "handle top-level mutable collections" in {
    mpjsons.deserialize[mutable.ArrayBuffer[Int]]("[1, 2]") mustBe mutable.ArrayBuffer(1, 2)
    mpjsons.deserialize[mutable.Map[Int, String]]("""[[1,"a"]]""") mustBe mutable.Map(1 -> "a")
    mpjsons.deserialize[mutable.HashSet[String]]("""["a","b"]""") mustBe mutable.HashSet("a", "b")
  }

  "Deserializer" must "still reject ListBuffer by design" in {
    val e = intercept[JsonInnerException] {
      mpjsons.deserialize[mutable.ListBuffer[Int]]("[1]")
    }
    e.getMessage must include("ListBuffer")
  }

  "Deserializer" must "report a clear error for a trait without a registered converter" in {
    val e = intercept[JsonInnerException] {
      mpjsons.deserialize[ShapeHolder]("""{"shape":{}}""")
    }
    e.getMessage must include("abstract")
    e.getMessage must include("Shape")
  }

  "Deserializer" must "report a clear error for an abstract class without a registered converter" in {
    val e = intercept[JsonInnerException] {
      mpjsons.deserialize[AnimalHolder]("""{"animal":{"name":"x"}}""")
    }
    e.getMessage must include("abstract")
    e.getMessage must include("Animal")
  }
}
