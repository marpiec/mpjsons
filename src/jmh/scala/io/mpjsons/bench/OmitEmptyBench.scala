package io.mpjsons.bench

import java.util.concurrent.TimeUnit

import io.mpjsons.omitEmpty
import org.openjdk.jmh.annotations._

import scala.collection.mutable

/** An event with collection, String and Option fields, as written by default. */
case class PlainEvent(id: Long, name: String, tags: List[String], labels: Set[String], attributes: Map[String, String],
                      comment: String, items: Vector[Long], discount: Option[Int])

/** The same event with `@omitEmpty` on the collection, String and Option fields. */
case class OmittingEvent(id: Long, name: String, @omitEmpty tags: List[String], @omitEmpty labels: Set[String],
                         @omitEmpty attributes: Map[String, String], @omitEmpty comment: String, @omitEmpty items: Vector[Long],
                         @omitEmpty discount: Option[Int])

/** A mutable collection: an absent key is read as `[]` again for each object. */
case class PlainBuffer(id: Long, buffer: mutable.ArrayBuffer[String])
case class OmittingBuffer(id: Long, @omitEmpty buffer: mutable.ArrayBuffer[String])

/**
 * Cost of `@omitEmpty` on write and read: the same values with and without the annotation, once with every collection
 * and String field empty (what the annotation is for) and once with all of them filled (the check without a gain).
 */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
@Fork(2)
class OmitEmptyBench {

  @Param(Array("empty", "full"))
  var values: String = _

  private val mpjsons = new io.mpjsons.MPJsons()
  private val plainWriter = mpjsons.buildStaticSerializer[PlainEvent]
  private val omittingWriter = mpjsons.buildStaticSerializer[OmittingEvent]
  private val plainReader = mpjsons.buildStaticDeserializer[PlainEvent]
  private val omittingReader = mpjsons.buildStaticDeserializer[OmittingEvent]
  private val plainBufferReader = mpjsons.buildStaticDeserializer[PlainBuffer]
  private val omittingBufferReader = mpjsons.buildStaticDeserializer[OmittingBuffer]

  var plain: PlainEvent = _
  var omitting: OmittingEvent = _
  var plainJson, omittingJson, plainBufferJson, omittingBufferJson: String = _

  @Setup
  def setup(): Unit = {
    val empty = values == "empty"
    plain = if (empty) PlainEvent(42, "renamed", Nil, Set.empty, Map.empty, "", Vector.empty, None)
    else PlainEvent(42, "renamed", List("a", "b"), Set("x"), Map("k" -> "v"), "a comment", Vector(1L, 2L, 3L), Some(5))
    omitting = OmittingEvent(plain.id, plain.name, plain.tags, plain.labels, plain.attributes, plain.comment, plain.items, plain.discount)
    plainJson = plainWriter.serialize(plain)
    omittingJson = omittingWriter.serialize(omitting)
    val buffer = if (empty) mutable.ArrayBuffer.empty[String] else mutable.ArrayBuffer("a")
    plainBufferJson = mpjsons.serialize(PlainBuffer(1, buffer))
    omittingBufferJson = mpjsons.serialize(OmittingBuffer(1, buffer))
    require(plainReader.deserialize(plainJson) == plain && omittingReader.deserialize(omittingJson) == omitting)
    println("\n" + values + ": plain " + plainJson.length + " chars " + plainJson + "\n" + values + ": @omitEmpty " +
      omittingJson.length + " chars " + omittingJson)
  }

  @Benchmark def writePlain(): String = plainWriter.serialize(plain)
  @Benchmark def writeOmitting(): String = omittingWriter.serialize(omitting)
  @Benchmark def readPlain(): AnyRef = plainReader.deserialize(plainJson)
  @Benchmark def readOmitting(): AnyRef = omittingReader.deserialize(omittingJson)
  @Benchmark def readPlainBuffer(): AnyRef = plainBufferReader.deserialize(plainBufferJson)
  @Benchmark def readOmittingBuffer(): AnyRef = omittingBufferReader.deserialize(omittingBufferJson)
}
