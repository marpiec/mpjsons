package io.mpjsons

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object OmitDefaultsSpec {
  case class Record(id: Long, count: Int, ratio: Double, active: Boolean, name: String, tags: List[String], note: Option[String], nested: Option[Record])

  class Bean {
    var port: Int = 8080
    var host: String = "localhost"
    var retries: Option[Int] = Some(3)
    var debug: Boolean = false
  }
}

/** MPJsonsConfig(omitDefaults = true): fields equal to what reading {} yields are left out; round trips are lossless. */
class OmitDefaultsSpec extends AnyFlatSpec {
  import OmitDefaultsSpec._

  val legacy = new MPJsons
  val omitting = new MPJsons(MPJsonsConfig(omitDefaults = true))
  val omittingCompact = new MPJsons(MPJsonsConfig(omitDefaults = true, compactOptions = true))

  "omitDefaults" must "leave out zero, false, null and None fields of case classes" in {
    val empty = Record(0L, 0, 0.0, active = false, null, null, None, None)
    omitting.serialize(empty) mustBe "{}"
    omitting.deserialize[Record]("{}") mustBe empty

    val full = Record(1L, 2, 0.5, active = true, "n", List("t"), Some("x"), Some(empty))
    omitting.serialize(full) mustBe """{"id":1,"count":2,"ratio":0.5,"active":true,"name":"n","tags":["t"],"note":{"value":"x"},"nested":{"value":{}}}"""
    omitting.deserialize[Record](omitting.serialize(full)) mustBe full

    val partial = Record(0L, 7, 0.0, active = false, "n", Nil, None, None)
    omitting.serialize(partial) mustBe """{"count":7,"name":"n","tags":[]}"""
    omitting.deserialize[Record](omitting.serialize(partial)) mustBe partial
  }

  "omitDefaults" must "use the no-arg constructor's initialisers as the defaults for beans" in {
    val bean = new Bean
    omitting.serialize(bean) mustBe "{}"
    legacy.serialize(bean) mustBe """{"port":8080,"host":"localhost","retries":{"value":3},"debug":false}"""

    bean.port = 9090
    bean.retries = None
    bean.debug = true
    omitting.serialize(bean) mustBe """{"port":9090,"retries":{},"debug":true}"""
    val back = omitting.deserialize[Bean](omitting.serialize(bean))
    back.port mustBe 9090
    back.host mustBe "localhost"
    back.retries mustBe None
    back.debug mustBe true

    // a value equal to the initialiser is omitted and comes back as the initialiser
    bean.port = 8080
    omitting.serialize(bean) mustBe """{"retries":{},"debug":true}"""
  }

  "omitDefaults" must "combine with compactOptions and still fail on non-default nulls under NullPolicy.Fail" in {
    val record = Record(0L, 0, 0.0, active = false, "n", null, Some("x"), None)
    omittingCompact.serialize(record) mustBe """{"name":"n","note":"x"}"""
    omittingCompact.deserialize[Record](omittingCompact.serialize(record)) mustBe record

    // host is null but the default is "localhost": null is not the default, so the null policy applies
    val bean = new Bean
    bean.host = null
    intercept[IllegalArgumentException] { omitting.serialize(bean) }
    new MPJsons(MPJsonsConfig(omitDefaults = true, nullPolicy = NullPolicy.WriteNull)).serialize(bean) mustBe """{"host":null}"""
  }

  "omitDefaults" must "not change the output of other types" in {
    omitting.serialize(List(0, 1)) mustBe "[0,1]"
    omitting.serialize(Map("a" -> 0)) mustBe """[["a",0]]"""
    omitting.serialize((0, false)) mustBe "[0,false]"
  }
}
