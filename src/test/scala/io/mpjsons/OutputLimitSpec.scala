package io.mpjsons

import java.nio.charset.StandardCharsets

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

object OutputLimitSpec {
  case class Note(title: String, tags: List[String])

  case class Stream(values: Iterable[Int])

  case class Numbers(values: Array[Int])

  case class Lookup(entries: Map[String, Int])

  case class Holder(inner: Note)
}

/** `MPJsonsConfig.maxOutputLength` and the `maxLength` argument stop output that grows too large. */
class OutputLimitSpec extends AnyFlatSpec {
  import OutputLimitSpec._

  private val sample = Note("zażółć", List("a", "b"))
  private val sampleJson = new MPJsons().serialize(sample)
  private val sampleBytes = sampleJson.getBytes(StandardCharsets.UTF_8).length // Polish letters take 2 bytes

  "The output limit" must "be 100 MiB by default and reject non-positive values" in {
    MPJsonsConfig().maxOutputLength mustBe 100 * 1024 * 1024
    an[IllegalArgumentException] must be thrownBy MPJsonsConfig(maxOutputLength = 0)
  }

  it must "allow output exactly as long as the limit, in characters and in bytes" in {
    val chars = new MPJsons(MPJsonsConfig(maxOutputLength = sampleJson.length))
    chars.serialize(sample) mustBe sampleJson
    val bytes = new MPJsons(MPJsonsConfig(maxOutputLength = sampleBytes))
    bytes.serializeToBytes(sample).length mustBe sampleBytes

    val e = the[JsonTooLargeException] thrownBy new MPJsons(MPJsonsConfig(maxOutputLength = sampleJson.length - 1)).serialize(sample)
    e.getMessage must include("io.mpjsons.OutputLimitSpec.Note")
    e.getMessage must include((sampleJson.length - 1) + " characters")
    val b = the[JsonTooLargeException] thrownBy new MPJsons(MPJsonsConfig(maxOutputLength = sampleBytes - 1)).serializeToBytes(sample)
    b.getMessage must include((sampleBytes - 1) + " bytes")
  }

  it must "be set per call by the maxLength argument, in every variant" in {
    val strict = new MPJsons(MPJsonsConfig(maxOutputLength = 10))
    val tpe = scala.reflect.runtime.universe.typeOf[Note]
    val name = classOf[Note].getName
    strict.serialize(sample, maxLength = 1000) mustBe sampleJson
    strict.serialize(sample, tpe, 1000) mustBe sampleJson
    strict.serialize(sample, name, 1000) mustBe sampleJson
    strict.serializeToBytes(sample, maxLength = 1000).length mustBe sampleBytes
    strict.serializeToBytes(sample, tpe, 1000).length mustBe sampleBytes
    strict.serializeToBytes(sample, name, 1000).length mustBe sampleBytes
    val static = strict.buildStaticSerializer[Note]
    static.serialize(sample, 1000) mustBe sampleJson
    static.serializeToStringBuilder(sample, 1000).toString mustBe sampleJson
    static.serializeToBytes(sample, 1000).length mustBe sampleBytes
    a[JsonTooLargeException] must be thrownBy static.serialize(sample)
    a[JsonTooLargeException] must be thrownBy static.serializeToBytes(sample)

    val default = new MPJsons
    a[JsonTooLargeException] must be thrownBy default.serialize(sample, maxLength = 10)
    a[JsonTooLargeException] must be thrownBy default.serializeToBytes(sample, tpe, 10)
    default.serialize(sample) mustBe sampleJson // the limit of a call does not stay
  }

  it must "stop a growing collection soon after the limit, not at its end" in {
    var produced = 0
    val endless = new Iterable[Int] {
      override def iterator: Iterator[Int] = Iterator.range(0, 50000000).map { i => produced += 1; i }
    }
    val m = new MPJsons(MPJsonsConfig(maxOutputLength = 10000))
    a[JsonTooLargeException] must be thrownBy m.serialize(Stream(endless))
    produced must be < 10000 // checked every 1024 elements; the whole sequence is 50 million
    produced = 0
    a[JsonTooLargeException] must be thrownBy m.serializeToBytes(Stream(endless))
    produced must be < 10000
  }

  it must "stop long strings, primitive arrays and maps" in {
    val m = new MPJsons(MPJsonsConfig(maxOutputLength = 5000))
    a[JsonTooLargeException] must be thrownBy m.serialize(Note("x" * 100000, Nil))
    a[JsonTooLargeException] must be thrownBy m.serializeToBytes(Note("x" * 100000, Nil))
    a[JsonTooLargeException] must be thrownBy m.serialize(Numbers(Array.fill(1000000)(7)))
    a[JsonTooLargeException] must be thrownBy m.serializeToBytes(Numbers(Array.fill(1000000)(7)))
    val lookup = Lookup((0 until 10000).map(i => ("k" + i) -> i).toMap)
    a[JsonTooLargeException] must be thrownBy m.serialize(lookup)
    a[JsonTooLargeException] must be thrownBy new MPJsons(MPJsonsConfig(maxOutputLength = 5000, mapsAsObjects = true)).serialize(lookup)
  }

  it must "give each nested serialize call its own limit and restore the outer one" in {
    val m = new MPJsons
    // a converter that serializes its value with the same instance, with a small limit of its own
    m.registerConverter[Note](_ => new JsonTypeSerializer[Note] {
      override def serialize(obj: Note, jsonBuilder: StringBuilder): Unit = jsonBuilder.append(m.serialize(obj.title, maxLength = 100))
    }, _ => null)
    m.serialize(Holder(Note("abc", Nil)), maxLength = 30) mustBe """{"inner":"abc"}"""
    a[JsonTooLargeException] must be thrownBy m.serialize(Holder(Note("abc", Nil)), maxLength = 10)
    a[JsonTooLargeException] must be thrownBy m.serialize(Holder(Note("x" * 200, Nil)))
    m.serialize(Holder(Note("abc", Nil))) mustBe """{"inner":"abc"}"""
  }
}
