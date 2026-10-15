package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object TypedNestedSpec {

  sealed trait SealedShape
  case class SealedCircle(r: Double) extends SealedShape
  case class SealedSquare(side: Int) extends SealedShape

  trait OpenShape
  case class OpenCircle(r: Double) extends OpenShape
  case object OpenPoint extends OpenShape

  case class Drawing(shapes: List[SealedShape], open: List[OpenShape])
}

/** Typed (polymorphic) converters for types nested inside objects, not only top-level classes. */
class TypedNestedSpec extends AnyFlatSpec {
  import TypedNestedSpec._

  "Typed converter" must "round trip subtypes of a sealed trait nested in an object" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[SealedShape]

    val shapes: List[SealedShape] = List(SealedCircle(1.5), SealedSquare(2))
    val json = mpjsons.serialize(shapes)
    json mustBe """[{"SealedCircle":{"r":1.5}},{"SealedSquare":{"side":2}}]"""
    mpjsons.deserialize[List[SealedShape]](json) mustBe shapes
  }

  "Typed converter" must "round trip subtypes of a non-sealed trait nested in an object" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[OpenShape]

    val shapes: List[OpenShape] = List(OpenCircle(1.5), OpenPoint)
    val json = mpjsons.serialize(shapes)
    json mustBe """[{"OpenCircle":{"r":1.5}},{"OpenPoint$":{}}]"""
    mpjsons.deserialize[List[OpenShape]](json) mustBe shapes
  }

  "Typed converter" must "work for typed fields inside beans" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[SealedShape]
    mpjsons.markTypedClass[OpenShape]

    val drawing = Drawing(List(SealedSquare(1)), List(OpenPoint, OpenCircle(2)))
    mpjsons.deserialize[Drawing](mpjsons.serialize(drawing)) mustBe drawing
  }

  "Typed converter" must "report an unknown type name clearly" in {
    val mpjsons = new MPJsons
    mpjsons.markTypedClass[SealedShape]

    val e = intercept[JsonInnerException] { mpjsons.deserialize[SealedShape]("""{"Triangle":{}}""") }
    e.getMessage must include("Triangle")
    e.getMessage must include("SealedShape")
  }
}
