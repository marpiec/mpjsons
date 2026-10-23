package io.mpjsons

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.values.StringDeserializer
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object SuperclassConverterSpec {
  trait Animal { def name: String }
  trait Pet extends Animal
  class Dog(val name: String) extends Pet
  class Fish(val name: String) extends Animal
}

/** When several registered superclass converters match a type, the most specific one (closest in the linearization) wins. */
class SuperclassConverterSpec extends AnyFlatSpec {
  import SuperclassConverterSpec._

  private def tagged(tag: String): MPJsons = {
    val mpjsons = new MPJsons
    mpjsons.registerSuperclassConverter[Animal](
      sf => (obj: Animal, jsonBuilder: StringBuilder) => { jsonBuilder.append('"').append("animal:").append(obj.name).append('"') },
      df => (jsonIterator: StringIterator) => new Fish(StringDeserializer.deserialize(jsonIterator).stripPrefix("animal:")))
    mpjsons.registerSuperclassConverter[Pet](
      sf => (obj: Pet, jsonBuilder: StringBuilder) => { jsonBuilder.append('"').append("pet:").append(obj.name).append('"') },
      df => (jsonIterator: StringIterator) => new Dog(StringDeserializer.deserialize(jsonIterator).stripPrefix("pet:")))
    mpjsons
  }

  "Superclass converters" must "prefer the most specific registered supertype regardless of registration order" in {
    val mpjsons = tagged("x")
    mpjsons.serialize(new Dog("Rex")) mustBe "\"pet:Rex\""
    mpjsons.serialize(new Fish("Nemo")) mustBe "\"animal:Nemo\""
    mpjsons.serialize[List[Animal]](List(new Dog("Rex"), new Fish("Nemo"))) mustBe """["animal:Rex","animal:Nemo"]"""

    mpjsons.deserialize[Dog]("\"pet:Rex\"").name mustBe "Rex"
    mpjsons.deserialize[Fish]("\"animal:Nemo\"").name mustBe "Nemo"
  }

  "Superclass converters" must "prefer the most specific supertype when registered in the opposite order" in {
    val mpjsons = new MPJsons
    mpjsons.registerSuperclassConverter[Pet](
      sf => (obj: Pet, jsonBuilder: StringBuilder) => { jsonBuilder.append("\"pet\"") },
      df => (jsonIterator: StringIterator) => { StringDeserializer.deserialize(jsonIterator); new Dog("pet") })
    mpjsons.registerSuperclassConverter[Animal](
      sf => (obj: Animal, jsonBuilder: StringBuilder) => { jsonBuilder.append("\"animal\"") },
      df => (jsonIterator: StringIterator) => { StringDeserializer.deserialize(jsonIterator); new Fish("animal") })

    mpjsons.serialize(new Dog("Rex")) mustBe "\"pet\""
    mpjsons.serialize(new Fish("Nemo")) mustBe "\"animal\""
    mpjsons.deserialize[Dog]("\"x\"").name mustBe "pet"
    mpjsons.deserialize[Fish]("\"x\"").name mustBe "animal"
  }
}
