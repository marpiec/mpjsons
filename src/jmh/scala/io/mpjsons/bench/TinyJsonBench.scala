package io.mpjsons.bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations._

import scala.reflect.runtime.universe._

case class TinyId(id: Long)

case class TinyText(v: String)

case class TinyPair(a: Int, b: Boolean)

case class TinyOption(id: Long, note: Option[String])

sealed trait TinyEvent

case class TinyCreated(id: Long) extends TinyEvent

case class TinyRenamed(id: Long, name: String) extends TinyEvent

/**
 * Many small documents (9-33 chars), as when events are read one by one: the fixed cost per call dominates. Each
 * document is read through the entry points applications use, for the 0.7.2 copy and the current implementation, in
 * mpjsons' compact form and as a jsonb column returns it.
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
class TinyJsonBench {

  @Param(Array("legacy", "current"))
  var impl: String = _

  /** compact: as mpjsons writes; jsonb: ", " and ": " separators, keys sorted (as read back from a jsonb column). */
  @Param(Array("compact", "jsonb"))
  var shape: String = _

  var idJson, textJson, pairJson, optionAbsentJson, optionPresentJson, eventJson: String = _

  var staticId, byTypeId, byTagId, byNameId, staticText, staticPair, staticOptionAbsent, staticOptionPresent, event, eventByType: String => AnyRef = _

  @Setup
  def setup(): Unit = {
    val jsonb = shape == "jsonb"
    idJson = if (jsonb) """{"id": 42}""" else """{"id":42}"""
    textJson = if (jsonb) """{"v": "abc"}""" else """{"v":"abc"}"""
    pairJson = if (jsonb) """{"a": 1, "b": true}""" else """{"a":1,"b":true}"""
    optionAbsentJson = if (jsonb) """{"id": 7}""" else """{"id":7}"""
    optionPresentJson = if (jsonb) """{"id": 7, "note": {"value": "x"}}""" else """{"id":7,"note":{"value":"x"}}"""
    eventJson = if (jsonb) """{"TinyCreated": {"id": 5}}""" else """{"TinyCreated":{"id":5}}"""

    val idType = typeOf[TinyId]
    val idName = classOf[TinyId].getName
    val eventType = typeOf[TinyEvent]
    if (impl == "legacy") {
      val m = new io.mpjsons.legacy.MPJsons()
      m.markTypedClass[TinyEvent]()
      staticId = staticOf(m.buildStaticDeserializer[TinyId])
      byTypeId = json => m.deserialize[TinyId](json, idType)
      byTagId = json => m.deserialize[TinyId](json)
      byNameId = json => m.deserialize[TinyId](json, idName)
      staticText = staticOf(m.buildStaticDeserializer[TinyText])
      staticPair = staticOf(m.buildStaticDeserializer[TinyPair])
      staticOptionAbsent = staticOf(m.buildStaticDeserializer[TinyOption])
      staticOptionPresent = staticOptionAbsent
      event = staticOf(m.buildStaticDeserializer[TinyEvent])
      eventByType = json => m.deserialize[TinyEvent](json, eventType)
    } else {
      val m = new io.mpjsons.MPJsons()
      m.markTypedClass[TinyEvent]()
      staticId = staticOf(m.buildStaticDeserializer[TinyId])
      byTypeId = json => m.deserialize[TinyId](json, idType)
      byTagId = json => m.deserialize[TinyId](json)
      byNameId = json => m.deserialize[TinyId](json, idName)
      staticText = staticOf(m.buildStaticDeserializer[TinyText])
      staticPair = staticOf(m.buildStaticDeserializer[TinyPair])
      staticOptionAbsent = staticOf(m.buildStaticDeserializer[TinyOption])
      staticOptionPresent = staticOptionAbsent
      event = staticOf(m.buildStaticDeserializer[TinyEvent])
      eventByType = json => m.deserialize[TinyEvent](json, eventType)
    }

    require(staticId(idJson) == TinyId(42) && byTypeId(idJson) == TinyId(42) && byTagId(idJson) == TinyId(42) && byNameId(idJson) == TinyId(42))
    require(staticText(textJson) == TinyText("abc") && staticPair(pairJson) == TinyPair(1, b = true))
    require(staticOptionAbsent(optionAbsentJson) == TinyOption(7, None) && staticOptionPresent(optionPresentJson) == TinyOption(7, Some("x")))
    require(event(eventJson) == TinyCreated(5) && eventByType(eventJson) == TinyCreated(5))
  }

  private def staticOf[T](d: io.mpjsons.legacy.StaticDeserializer[T]): String => AnyRef = json => d.deserialize(json).asInstanceOf[AnyRef]

  private def staticOf[T](d: io.mpjsons.StaticDeserializer[T]): String => AnyRef = json => d.deserialize(json).asInstanceOf[AnyRef]

  @Benchmark def idStatic(): AnyRef = staticId(idJson)
  @Benchmark def idByType(): AnyRef = byTypeId(idJson)
  @Benchmark def idByTypeTag(): AnyRef = byTagId(idJson)
  @Benchmark def idByTypeName(): AnyRef = byNameId(idJson)
  @Benchmark def text(): AnyRef = staticText(textJson)
  @Benchmark def pair(): AnyRef = staticPair(pairJson)
  @Benchmark def optionAbsent(): AnyRef = staticOptionAbsent(optionAbsentJson)
  @Benchmark def optionPresent(): AnyRef = staticOptionPresent(optionPresentJson)
  @Benchmark def typedEvent(): AnyRef = event(eventJson)
  @Benchmark def typedEventByType(): AnyRef = eventByType(eventJson)
}
