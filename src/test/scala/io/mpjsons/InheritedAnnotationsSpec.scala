package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object InheritedAnnotationsSpec {
  trait Named { @jsonName("n") def name: String }
  case class Person(name: String, age: Int) extends Named

  // the field's own annotation wins over a supertype's
  case class OwnName(@jsonName("own") name: String) extends Named

  // a nearer supertype wins over a farther one
  trait NearlyNamed extends Named { @jsonName("near") def name: String }
  case class NearName(name: String) extends NearlyNamed

  // linearization of Both: Both, B, A, so B is nearer
  trait A { @jsonName("a") def value: Int }
  trait B { @jsonName("b") def value: Int }
  case class Both(value: Int) extends A with B

  trait Secret { @skipSerialization def token: String }
  case class Session(user: String, token: String) extends Secret

  trait Commented { @nullable def comment: String }
  case class Note(id: Int, comment: String) extends Commented

  abstract class Identified { @jsonName("i") def id: Long }
  case class Item(id: Long, label: String) extends Identified

  sealed trait Event { @jsonName("at") def time: Long }
  case class Started(time: Long) extends Event
  case class Stopped(time: Long, reason: String) extends Event

  // the README example
  sealed trait ReadmeEvent {
    @jsonName("at") def time: Long
    @omitEmpty def tags: Set[String]
  }
  case class Created(time: Long, tags: Set[String]) extends ReadmeEvent
  case class Renamed(time: Long, name: String, tags: Set[String]) extends ReadmeEvent
}

/** Field annotations on a member of a trait or superclass apply to the field that implements it. */
class InheritedAnnotationsSpec extends AnyFlatSpec {
  import InheritedAnnotationsSpec._

  private val mpjsons = new MPJsons
  private val viaConstructor = new MPJsons(MPJsonsConfig(caseClassesViaConstructor = true))

  "@jsonName on a supertype member" must "name the key of the implementing field, with the Scala name as fallback" in {
    for (m <- Seq(mpjsons, viaConstructor)) {
      m.serialize(Person("ann", 3)) mustBe """{"n":"ann","age":3}"""
      m.deserialize[Person]("""{"n":"ann","age":3}""") mustBe Person("ann", 3)
      m.deserialize[Person]("""{"name":"ann","age":3}""") mustBe Person("ann", 3)
      m.serialize(Item(7, "x")) mustBe """{"i":7,"label":"x"}"""
      m.deserialize[Item]("""{"i":7,"label":"x"}""") mustBe Item(7, "x")
    }
  }

  it must "give way to the field's own annotation and to a nearer supertype's" in {
    mpjsons.serialize(OwnName("x")) mustBe """{"own":"x"}"""
    mpjsons.serialize(NearName("x")) mustBe """{"near":"x"}"""
    mpjsons.serialize(Both(1)) mustBe """{"b":1}"""
    mpjsons.deserialize[Both]("""{"b":1}""") mustBe Both(1)
  }

  it must "apply across a typed hierarchy" in {
    val typed = new MPJsons
    typed.markTypedClass[Event]()
    val events: List[Event] = List(Started(1), Stopped(2, "done"))
    val json = typed.serialize(events)
    json mustBe """[{"Started":{"at":1}},{"Stopped":{"at":2,"reason":"done"}}]"""
    typed.deserialize[List[Event]](json) mustBe events
  }

  it must "combine with other annotations of the supertype, as the README shows" in {
    mpjsons.serialize(Created(5, Set.empty)) mustBe """{"at":5}"""
    mpjsons.serialize(Renamed(6, "", Set("x"))) mustBe """{"at":6,"name":"","tags":["x"]}"""
    mpjsons.deserialize[Created]("""{"at":5}""") mustBe Created(5, Set.empty)
  }

  "@skipSerialization on a supertype member" must "leave the implementing field out" in {
    mpjsons.serialize(Session("ann", "secret")) mustBe """{"user":"ann"}"""
    mpjsons.deserialize[Session]("""{"user":"ann","token":"secret"}""") mustBe Session("ann", null)
  }

  "@nullable on a supertype member" must "allow a null in the implementing field" in {
    mpjsons.serialize(Note(1, null)) mustBe """{"id":1}"""
    mpjsons.deserialize[Note]("""{"id":1,"comment":null}""") mustBe Note(1, null)
  }
}
