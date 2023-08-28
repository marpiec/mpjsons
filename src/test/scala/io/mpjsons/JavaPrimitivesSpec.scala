package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import scala.annotation.meta.field

case class PrimitiveHolder(
   @(nullable @field) a: java.lang.Long,
   @(nullable @field) b: java.lang.Integer,
   @(nullable @field) c: java.lang.Short,
   @(nullable @field) d: java.lang.Byte,
   @(nullable @field) e: java.lang.Boolean,
   @(nullable @field) f: java.lang.Double,
   @(nullable @field) g: java.lang.Float,
   @(nullable @field) h: java.lang.Character
)

class JavaPrimitivesSpec extends AnyFlatSpec {

  val mpjsons = new MPJsons

  "Serializer" must "handle filled java primitives" in {

    val o = PrimitiveHolder(a = 123L, b = 21, c = new java.lang.Short(5.toShort), d = new java.lang.Byte(65.toByte), e = true, f = 0.45, g = 0.75f, h = 'g')

    val serialized = mpjsons.serialize(o)

    serialized must be ("""{"h":"g","g":0.75,"f":0.45,"e":true,"d":65,"c":5,"b":21,"a":123}""")

    val deserialized = mpjsons.deserialize[PrimitiveHolder](serialized)

    deserialized must be (o)
  }

  "Serializer" must "handle null java primitives" in {

    val o = PrimitiveHolder(a = null, b = null, c = null, d = null, e = null, f = null, g = null, h = null)

    val serialized = mpjsons.serialize(o)

    serialized must be ("""{}""")

    val deserialized = mpjsons.deserialize[PrimitiveHolder](serialized)

    deserialized must be (o)
  }
}
