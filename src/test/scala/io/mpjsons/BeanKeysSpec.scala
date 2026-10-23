package io.mpjsons

import io.mpjsons.impl.JsonInnerException
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object BeanKeysSpec {
  case class Keys(ab: Int, abc: Int, a: Int, longerFieldName: String)
  case class Empty()
  case class Wide(f1: Int, f2: Int, f3: Int, f4: Int, f5: Int, f6: Int, f7: Int, f8: Int, f9: Int, f10: Int,
                  f11: Int, f12: Int, f13: Int, f14: Int, f15: Int, f16: Int, f17: Int)
}

class BeanKeysSpec extends AnyFlatSpec {
  import BeanKeysSpec._

  val mpjsons = new MPJsons
  val lenient = new MPJsons(ignoreNonExistingFields = true)

  "Deserializer" must "match quoted, bare and escaped keys" in {
    mpjsons.deserialize[Keys]("""{"ab":1,"abc":2,"a":3,"longerFieldName":"x"}""") mustBe Keys(1, 2, 3, "x")
    mpjsons.deserialize[Keys]("""{ ab : 1 , abc:2 ,a: 3,longerFieldName:"x" }""") mustBe Keys(1, 2, 3, "x")
    mpjsons.deserialize[Keys]("""{"ab":1,"abc":2,"a":3,"longerFieldName":"x"}""") mustBe Keys(1, 2, 3, "x")
  }

  "Deserializer" must "report unknown keys by name in strict mode" in {
    val e = intercept[JsonInnerException] { mpjsons.deserialize[Keys]("""{"ab":1,"zzz":2}""") }
    e.getMessage must include("No field [zzz]")
    val e2 = intercept[JsonInnerException] { mpjsons.deserialize[Keys]("""{"ab":1, zzz :2}""") }
    e2.getMessage must include("No field [zzz]")
    val e3 = intercept[JsonInnerException] { mpjsons.deserialize[Keys]("""{"az":1}""") }
    e3.getMessage must include("No field [az]")
  }

  "Deserializer" must "skip unknown keys of every value kind in lenient mode" in {
    val json = """{"zzz":{"q":[1,{"w":null}]},"ab":1,"yyy":"s\"t","abc":2,"xxx":-1.5e3,"a":3,"www":true,"longerFieldName":"x","vvv":null}"""
    lenient.deserialize[Keys](json) mustBe Keys(1, 2, 3, "x")
  }

  "Deserializer" must "handle empty and whitespace-only objects" in {
    mpjsons.deserialize[Empty]("{}") mustBe Empty()
    mpjsons.deserialize[Empty](" {  } ") mustBe Empty()
    lenient.deserialize[Keys]("{ }") mustBe Keys(0, 0, 0, null)
  }

  "Deserializer" must "handle beans with many fields" in {
    val wide = Wide(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17)
    mpjsons.deserialize[Wide](mpjsons.serialize(wide)) mustBe wide
  }

  "Deserializer" must "fail on an unterminated key" in {
    intercept[JsonInnerException] { mpjsons.deserialize[Keys]("""{"ab""") }
  }
}
