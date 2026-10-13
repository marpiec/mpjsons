package io.mpjsons.bench

import io.mpjsons.MPJsons
import org.openjdk.jmh.annotations._
import org.openjdk.jmh.infra.Blackhole

import java.util.concurrent.TimeUnit

/**
 * JMH benchmarks for the steady-state (warm cache) cost of serialization and deserialization.
 *
 * Run all of them with:
 *   sbt "Jmh/run -f1 -wi 3 -i 5 .*MpjsonsBenchmark.*"
 * Add "-prof gc" to see allocation per operation. See docs/performance.md for baseline numbers.
 */
object MpjsonsBenchmark {

  class InnerObject {
    var intValue: Int = _
    var stringValue: String = _
  }

  class SimpleDataObject {
    var charValue: Char = _
    var longValue: Long = _
    var intValue: Int = _
    var stringValue: String = _
    var booleanValue: Boolean = _
    var innerObject: InnerObject = _
    var arrayObject: Array[String] = _
    var arrayPrimitive: Array[Long] = _
    var listObject: List[String] = _
    var listPrimitive: List[Int] = _
    var emptyArray: Array[Long] = _
  }

  case class Flat(a: Int, b: Long, c: String, d: Boolean, e: Double)

  case class Address(street: String, city: String, zip: Option[String])
  case class Person(name: String, age: Int, emails: List[String], address: Address, tags: Map[String, Int])
}

@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 2)
@Measurement(iterations = 5, time = 2)
@Fork(1)
class MpjsonsBenchmark {
  import MpjsonsBenchmark._

  val mpjsons = new MPJsons

  val sdo: SimpleDataObject = {
    val o = new SimpleDataObject
    o.charValue = 'M'
    o.longValue = 1234567891234L
    o.intValue = 1111
    o.stringValue = "Hello Json \n\" parser\\serializer \""
    o.booleanValue = true
    o.innerObject = new InnerObject
    o.innerObject.intValue = -2222
    o.innerObject.stringValue = "inner string"
    o.arrayObject = Array("Be", "or", "not to be")
    o.arrayPrimitive = Array(6L, 8L, 10L)
    o.listObject = List("Hello", "json", "serializer")
    o.listPrimitive = List(15, 30, 1)
    o.emptyArray = Array()
    o
  }
  val sdoSerializer = mpjsons.buildStaticSerializer[SimpleDataObject]
  val sdoDeserializer = mpjsons.buildStaticDeserializer[SimpleDataObject]
  val sdoJson: String = sdoSerializer.serialize(sdo)

  val flat = Flat(1, 2L, "abc", true, 1.5)
  val flatSerializer = mpjsons.buildStaticSerializer[Flat]
  val flatDeserializer = mpjsons.buildStaticDeserializer[Flat]
  val flatJson: String = flatSerializer.serialize(flat)

  val person = Person("Ann Example", 42, List("ann@example.com", "a.example@example.org"),
    Address("1 Main St", "Springfield", Some("12345")), Map("a" -> 1, "b" -> 2))
  val personSerializer = mpjsons.buildStaticSerializer[Person]
  val personDeserializer = mpjsons.buildStaticDeserializer[Person]
  val personJson: String = personSerializer.serialize(person)

  val longString: String = "The quick brown fox jumps over the lazy dog. " * 5 // 225 chars, no escapes
  val stringSerializer = mpjsons.buildStaticSerializer[String]
  val stringDeserializer = mpjsons.buildStaticDeserializer[String]
  val longStringJson: String = stringSerializer.serialize(longString)

  val ints: List[Int] = (1 to 100).toList
  val intsSerializer = mpjsons.buildStaticSerializer[List[Int]]
  val intsDeserializer = mpjsons.buildStaticDeserializer[List[Int]]
  val intsJson: String = intsSerializer.serialize(ints)

  @Benchmark def sdoSerializeTypeTagApi(bh: Blackhole): Unit = bh.consume(mpjsons.serialize(sdo))
  @Benchmark def sdoSerializeStatic(bh: Blackhole): Unit = bh.consume(sdoSerializer.serialize(sdo))
  @Benchmark def sdoDeserializeTypeTagApi(bh: Blackhole): Unit = bh.consume(mpjsons.deserialize[SimpleDataObject](sdoJson))
  @Benchmark def sdoDeserializeStatic(bh: Blackhole): Unit = bh.consume(sdoDeserializer.deserialize(sdoJson))

  @Benchmark def flatSerializeStatic(bh: Blackhole): Unit = bh.consume(flatSerializer.serialize(flat))
  @Benchmark def flatDeserializeStatic(bh: Blackhole): Unit = bh.consume(flatDeserializer.deserialize(flatJson))

  @Benchmark def personSerializeStatic(bh: Blackhole): Unit = bh.consume(personSerializer.serialize(person))
  @Benchmark def personDeserializeStatic(bh: Blackhole): Unit = bh.consume(personDeserializer.deserialize(personJson))

  @Benchmark def string225SerializeStatic(bh: Blackhole): Unit = bh.consume(stringSerializer.serialize(longString))
  @Benchmark def string225DeserializeStatic(bh: Blackhole): Unit = bh.consume(stringDeserializer.deserialize(longStringJson))

  @Benchmark def listInt100SerializeStatic(bh: Blackhole): Unit = bh.consume(intsSerializer.serialize(ints))
  @Benchmark def listInt100DeserializeStatic(bh: Blackhole): Unit = bh.consume(intsDeserializer.deserialize(intsJson))
}
