package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

// Top-level in package io.mpjsons, next to the typed classes used below, so the sibling-name lookup can reach them.
object TripwireState {
  @volatile var initialized = false
}

object Tripwire {
  TripwireState.initialized = true
}

trait OpenBase
case class OpenChild(a: Int) extends OpenBase

/** The class named in a typed wrapper comes from the JSON, so resolving it must never escape the marked hierarchy. */
class TypedResolutionSafetySpec extends AnyFlatSpec {

  "Typed deserializer" must "reject a class from the same package that is not a subtype of the marked type" in {
    val sealedTyped = new MPJsons
    sealedTyped.markTypedClass[Shape]
    val e = intercept[JsonInnerException] { sealedTyped.deserialize[List[Shape]]("""[{"InnerObject":{"intValue":1}}]""") }
    e.getMessage must include("not a subtype")
    e.getMessage must include("InnerObject")

    val openTyped = new MPJsons
    openTyped.markTypedClass[OpenBase]
    intercept[JsonInnerException] { openTyped.deserialize[OpenBase]("""{"InnerObject":{"intValue":1}}""") }.getMessage must include("not a subtype")
    openTyped.deserialize[OpenBase]("""{"OpenChild":{"a":1}}""") mustBe OpenChild(1)
  }

  "Typed deserializer" must "not run the static initializer of a class named in the JSON" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[OpenBase]
    intercept[JsonInnerException] { mpjsons.deserialize[OpenBase]("""{"Tripwire$":{}}""") }.getMessage must include("not a subtype")
    TripwireState.initialized mustBe false
  }

  "Typed deserializer" must "reject names that would leave the marked type's package" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[OpenBase]
    for (name <- List("impl.JsonInnerException", "..OpenChild", "a/b", "[LOpenChild;", "")) {
      intercept[JsonInnerException] { mpjsons.deserialize[OpenBase]("{\"" + name + "\":{}}") }.getMessage must include("Invalid type name")
    }
  }

  "Typed deserializer" must "report unknown and wrongly cased names as unknown, not as linkage errors" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[OpenBase]
    intercept[JsonInnerException] { mpjsons.deserialize[OpenBase]("""{"NoSuchChild":{}}""") }.getMessage must include("Unknown type name")
    intercept[JsonInnerException] { mpjsons.deserialize[OpenBase]("""{"openchild":{"a":1}}""") }.getMessage must include("Unknown type name")
  }
}
