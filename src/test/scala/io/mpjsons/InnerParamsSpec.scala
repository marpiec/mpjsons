package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object InnerParamsSpec {

  case class InnerType(text: String)

  case class SomeType[T](content: Option[T])

  case class MainType(content: SomeType[InnerType])

}

class InnerParamsSpec extends AnyFlatSpec {
  import InnerParamsSpec._

  val mpjsons = new MPJsons

  "Serializer" must "handle many nested parametrized types" in {

    val obj = MainType(SomeType(Some(InnerType("Hello"))))

    val serialized = mpjsons.serialize(obj)

    val deserialized = mpjsons.deserialize[MainType](serialized)

    deserialized mustBe obj
  }
}
