package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers.convertToAnyMustWrapper

case class Wrapper[T](value: T)


class WrapperSerializer extends JsonTypeSerializer[Wrapper[_]] {
  override def serialize(obj: Wrapper[_], jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{\"someValue\":")
    jsonBuilder.append(obj.value.toString)
    jsonBuilder.append("}")
  }
}

class CustomSerializerAnySubType extends AnyFlatSpec {
  val mpjsons = new MPJsons

  mpjsons.registerSerializerForAnySubtype[Wrapper[_]](sf => new WrapperSerializer)

  "Serializer" must "handle Wrapper" in {

    mpjsons.serialize(Wrapper(123)) mustBe """{"someValue":123}"""

    mpjsons.serialize(Wrapper("Hello")) mustBe """{"someValue":Hello}"""

  }

}
