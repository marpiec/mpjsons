package io.mpjsons

import java.nio.charset.StandardCharsets

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import scala.collection.mutable

object OmitEmptySpec {
  case class Order(id: Long, @omitEmpty tags: Set[String], @omitEmpty items: List[Int], @omitEmpty note: String,
                   @omitEmpty attributes: Map[String, Int], @omitEmpty codes: Vector[String])

  case class WithArray(@omitEmpty values: Array[Int], @omitEmpty names: Array[String])

  case class WithMutable(@omitEmpty buffer: mutable.ArrayBuffer[Int], @omitEmpty map: mutable.Map[String, Int])

  class Bean {
    var id: Int = 0
    @omitEmpty var tags: List[String] = _
    @omitEmpty var note: String = _
  }

  class BeanWithInitialiser {
    @omitEmpty var tags: List[String] = List("initial")
  }

  case class Node(name: String, @omitEmpty children: List[Node])

  case class WrongType(@omitEmpty id: Int)
  case class WrongOption(@omitEmpty value: Option[List[Int]])

  case class Renamed(@omitEmpty @jsonName("t") tags: List[String], id: Int)

  // @omitEmpty on a member of a supertype applies to the field that implements it
  trait Tagged { @omitEmpty def tags: Set[String] }
  case class TaggedOrder(id: Long, tags: Set[String]) extends Tagged

  trait Noted { @omitEmpty val note: String }
  case class NotedOrder(id: Long, note: String) extends Noted

  abstract class Labelled { @omitEmpty def labels: List[String] }
  case class LabelledOrder(id: Long, labels: List[String]) extends Labelled

  trait Middle extends Tagged
  case class DeepOrder(id: Long, tags: Set[String]) extends Middle

  sealed trait Event { @omitEmpty def aliases: List[String] }
  case class Created(id: Long, aliases: List[String]) extends Event
  case class Retitled(id: Long, title: String, aliases: List[String]) extends Event

  trait Counted { @omitEmpty def count: Int }
  case class CountedImpl(count: Int) extends Counted
}

class OmitEmptySpec extends AnyFlatSpec {
  import OmitEmptySpec._

  private val mpjsons = new MPJsons
  private val viaConstructor = new MPJsons(MPJsonsConfig(caseClassesViaConstructor = true))

  private val emptyOrder = Order(1, Set.empty, Nil, "", Map.empty, Vector.empty)
  private val fullOrder = Order(2, Set("a"), List(1, 2), "n", Map("k" -> 3), Vector("c"))

  "@omitEmpty" must "leave empty collections, maps and strings out of the output" in {
    mpjsons.serialize(emptyOrder) mustBe """{"id":1}"""
    mpjsons.serialize(fullOrder) mustBe """{"id":2,"tags":["a"],"items":[1,2],"note":"n","attributes":[["k",3]],"codes":["c"]}"""
    new String(mpjsons.serializeToBytes(emptyOrder), StandardCharsets.UTF_8) mustBe mpjsons.serialize(emptyOrder)
    new String(mpjsons.serializeToBytes(fullOrder), StandardCharsets.UTF_8) mustBe mpjsons.serialize(fullOrder)
  }

  it must "read missing keys as empty values, in both bean modes" in {
    for (m <- Seq(mpjsons, viaConstructor)) {
      m.deserialize[Order]("""{"id":1}""") mustBe emptyOrder
      m.deserialize[Order](m.serialize(fullOrder)) mustBe fullOrder
      m.deserialize[Order]("""{"id":1,"tags":[],"items":[],"note":"","attributes":[],"codes":[]}""") mustBe emptyOrder
    }
  }

  it must "omit empty arrays and read them back as empty arrays of the right type" in {
    mpjsons.serialize(WithArray(Array.empty, Array.empty)) mustBe "{}"
    for (m <- Seq(mpjsons, viaConstructor)) {
      val read = m.deserialize[WithArray]("{}")
      read.values mustBe Array.empty[Int]
      read.names mustBe Array.empty[String]
    }
  }

  it must "give every object its own empty mutable collection" in {
    for (m <- Seq(mpjsons, viaConstructor)) {
      val first = m.deserialize[WithMutable]("{}")
      val second = m.deserialize[WithMutable]("{}")
      first.buffer mustBe empty
      first.buffer must not be theSameInstanceAs(second.buffer)
      first.map must not be theSameInstanceAs(second.map)
      first.buffer += 1
      second.buffer mustBe empty
    }
  }

  it must "work on bean fields, and keep a value set by a no-arg constructor" in {
    val bean = new Bean
    bean.tags = Nil
    bean.note = ""
    mpjsons.serialize(bean) mustBe """{"id":0}"""
    val read = mpjsons.deserialize[Bean]("""{"id":5}""")
    read.tags mustBe Nil
    read.note mustBe ""
    mpjsons.deserialize[BeanWithInitialiser]("{}").tags mustBe List("initial")
  }

  it must "treat null like before: by the null policy, not as empty" in {
    val withNull = Order(1, null, Nil, null, Map.empty, Vector.empty)
    an[IllegalArgumentException] must be thrownBy mpjsons.serialize(withNull)
    val omitting = new MPJsons(MPJsonsConfig(nullPolicy = NullPolicy.Omit))
    omitting.serialize(withNull) mustBe """{"id":1}"""
    val writingNull = new MPJsons(MPJsonsConfig(nullPolicy = NullPolicy.WriteNull))
    writingNull.serialize(withNull) mustBe """{"id":1,"tags":null,"note":null}"""
    writingNull.deserialize[Order]("""{"id":1,"tags":null,"note":null}""") mustBe withNull
  }

  it must "not let omitDefaults drop a null that would read back as empty" in {
    // Reading {} gives Order(0, Set(), Nil, "", Map(), Vector()): null is not that, so it is written
    val m = new MPJsons(MPJsonsConfig(omitDefaults = true, nullPolicy = NullPolicy.WriteNull))
    val withNull = Order(0, null, Nil, null, Map.empty, Vector.empty)
    m.serialize(withNull) mustBe """{"tags":null,"note":null}"""
    m.deserialize[Order](m.serialize(withNull)) mustBe withNull
    m.serialize(Order(0, Set.empty, Nil, "", Map.empty, Vector.empty)) mustBe "{}"
  }

  it must "read an absent key the way [] or \"\" reads, through registered transforms" in {
    val m = new MPJsons
    m.postDeserializationTransform[String](s => if (s.isEmpty) "<empty>" else s)
    m.deserialize[Order]("""{"id":1}""").note mustBe "<empty>"
    m.deserialize[Order]("""{"id":1,"note":""}""").note mustBe "<empty>"
  }

  it must "work with the object form of maps, renamed keys and recursive types" in {
    val objects = new MPJsons(MPJsonsConfig(mapsAsObjects = true))
    objects.serialize(fullOrder) mustBe """{"id":2,"tags":["a"],"items":[1,2],"note":"n","attributes":{"k":3},"codes":["c"]}"""
    objects.deserialize[Order]("""{"id":1}""") mustBe emptyOrder
    mpjsons.serialize(Renamed(Nil, 1)) mustBe """{"id":1}"""
    mpjsons.deserialize[Renamed]("""{"t":["x"],"id":1}""") mustBe Renamed(List("x"), 1)
    val tree = Node("a", List(Node("b", Nil)))
    mpjsons.serialize(tree) mustBe """{"name":"a","children":[{"name":"b"}]}"""
    mpjsons.deserialize[Node]("""{"name":"a","children":[{"name":"b"}]}""") mustBe tree
  }

  it must "apply to fields implementing an annotated member of a trait or superclass" in {
    for (m <- Seq(mpjsons, viaConstructor)) {
      m.serialize(TaggedOrder(1, Set.empty)) mustBe """{"id":1}"""
      m.deserialize[TaggedOrder]("""{"id":1}""") mustBe TaggedOrder(1, Set.empty)
      m.serialize(NotedOrder(1, "")) mustBe """{"id":1}"""
      m.deserialize[NotedOrder]("""{"id":1}""") mustBe NotedOrder(1, "")
      m.serialize(LabelledOrder(1, Nil)) mustBe """{"id":1}"""
      m.deserialize[LabelledOrder]("""{"id":1}""") mustBe LabelledOrder(1, Nil)
      m.serialize(DeepOrder(1, Set("a"))) mustBe """{"id":1,"tags":["a"]}"""
      m.deserialize[DeepOrder]("""{"id":1}""") mustBe DeepOrder(1, Set.empty)
    }
  }

  it must "apply across a typed hierarchy, to the annotated member only" in {
    val typed = new MPJsons
    typed.markTypedClass[Event]()
    val events: List[Event] = List(Created(1, Nil), Retitled(2, "", Nil), Created(3, List("c")))
    val json = typed.serialize(events)
    json mustBe """[{"Created":{"id":1}},{"Retitled":{"id":2,"title":""}},{"Created":{"id":3,"aliases":["c"]}}]"""
    typed.deserialize[List[Event]](json) mustBe events
  }

  it must "fail when the (de)serializer is built for a field that cannot be empty" in {
    the[IllegalArgumentException] thrownBy mpjsons.serialize(CountedImpl(1)) must have message
      "@omitEmpty on field count of io.mpjsons.OmitEmptySpec.CountedImpl: it applies to collections, arrays and String, not to Int"
    val wrongType = the[JsonInnerException] thrownBy mpjsons.deserialize[WrongType]("""{"id":1}""")
    wrongType.getMessage must include("@omitEmpty on field id")
    the[IllegalArgumentException] thrownBy mpjsons.serialize(WrongOption(None)) must have message
      "@omitEmpty on field value of io.mpjsons.OmitEmptySpec.WrongOption: it applies to collections, arrays and String, not to Option[List[Int]]"
  }
}
