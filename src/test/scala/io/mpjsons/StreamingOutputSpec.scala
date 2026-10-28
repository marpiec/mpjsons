package io.mpjsons

import java.io.{ByteArrayOutputStream, IOException, OutputStream}
import java.nio.charset.StandardCharsets

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers._

import scala.collection.mutable

object StreamingOutputSpec {
  case class Line(id: Int, name: String, price: Double, tags: List[String], attributes: Map[String, Int])

  case class Document(title: String, lines: List[Line])

  // empty values that are not shared instances are written and then taken back, possibly across a chunk boundary
  case class Inner(@omitEmpty values: mutable.ArrayBuffer[Int],
                   @omitEmpty @jsonName("aRatherLongNameSoThatAChunkBoundaryFallsInsideIt") more: mutable.ArrayBuffer[Int])

  case class Entry(id: Int, @omitEmpty inner: Option[Inner], @omitEmpty note: mutable.ArrayBuffer[String], last: Int)

  case class Padded(pad: String, entries: List[Entry])

  case class Big(@omitEmpty numbers: mutable.ArrayBuffer[Int])

  case class Money(cents: Long)
  case class Priced(name: String, prices: List[Money])

  /** Records each write; fails or closes nothing unless asked. */
  class RecordingStream(failAfter: Int = Int.MaxValue) extends OutputStream {
    val bytes = new ByteArrayOutputStream()
    var writes = 0
    var largestWrite = 0
    var flushes = 0
    var closed = false

    override def write(b: Int): Unit = write(Array(b.toByte), 0, 1)

    override def write(b: Array[Byte], off: Int, len: Int): Unit = {
      if (bytes.size + len > failAfter) {
        throw new IOException("Connection reset")
      }
      writes += 1
      largestWrite = Math.max(largestWrite, len)
      bytes.write(b, off, len)
    }

    override def flush(): Unit = flushes += 1

    override def close(): Unit = closed = true
  }
}

/** `serializeToStream` writes the same bytes as `serializeToBytes` in chunks; `serializeToByteBuffer` wraps them. */
class StreamingOutputSpec extends AnyFlatSpec {
  import StreamingOutputSpec._

  private val m = new MPJsons

  private val document = Document("Zażółć gęślą jaźń 😀", (0 until 5000).toList.map { i =>
    Line(i, "pozycja " + i + " ąę\"/\\", i * 1.25, List("a", "żółw", "c" * (i % 7)), Map("k" + i -> i, "x" -> -i))
  })

  private def streamed[T](obj: T, mpjsons: MPJsons = m)(implicit tag: scala.reflect.runtime.universe.TypeTag[T]): RecordingStream = {
    val stream = new RecordingStream
    mpjsons.serializeToStream(obj, stream)
    stream
  }

  "serializeToStream" must "write the bytes of serializeToBytes in chunks of 64 KiB, flush and not close the stream" in {
    val expected = m.serializeToBytes(document)
    expected.length must be > 500000
    val stream = streamed(document)
    stream.bytes.toByteArray mustBe expected
    stream.writes must be > 5
    stream.largestWrite must be <= 128 * 1024
    stream.flushes mustBe 1
    stream.closed mustBe false
  }

  it must "write chunks of MPJsonsConfig.streamChunkBytes" in {
    MPJsonsConfig().streamChunkBytes mustBe 64 * 1024
    an[IllegalArgumentException] must be thrownBy MPJsonsConfig(streamChunkBytes = 0)
    val expected = m.serializeToBytes(document)
    val small = new RecordingStream
    new MPJsons(MPJsonsConfig(streamChunkBytes = 4096)).serializeToStream(document, small)
    small.bytes.toByteArray mustBe expected
    small.writes must be > expected.length / 8192
    small.largestWrite must be <= 8192
    val static = new RecordingStream
    new MPJsons(MPJsonsConfig(streamChunkBytes = 4096)).buildStaticSerializer[Document].serializeToStream(document, static)
    static.largestWrite must be <= 8192
    // a chunk smaller than one value: every write that does not fit goes to the stream first
    val tiny = new MPJsons(MPJsonsConfig(streamChunkBytes = 1, compactOptions = true))
    val entries = Padded("p", (0 until 300).toList.map(i =>
      Entry(i, Some(Inner(mutable.ArrayBuffer.empty, mutable.ArrayBuffer.empty)), mutable.ArrayBuffer.empty, i)))
    streamed(entries, tiny).bytes.toByteArray mustBe tiny.serializeToBytes(entries)
  }

  it must "write the same bytes in every variant" in {
    val expected = m.serializeToBytes(document)
    val tpe = scala.reflect.runtime.universe.typeOf[Document]
    val byType = new RecordingStream
    m.serializeToStream(document, tpe, byType)
    byType.bytes.toByteArray mustBe expected
    val byName = new RecordingStream
    m.serializeToStream(document, classOf[Document].getName, byName)
    byName.bytes.toByteArray mustBe expected
    val static = new RecordingStream
    m.buildStaticSerializer[Document].serializeToStream(document, static)
    static.bytes.toByteArray mustBe expected
    streamed(Line(1, "small", 2.5, Nil, Map.empty)).bytes.toByteArray mustBe m.serializeToBytes(Line(1, "small", 2.5, Nil, Map.empty))
  }

  it must "take back empty @omitEmpty values wherever a chunk boundary falls" in {
    val entries = (0 until 3000).toList.map { i =>
      val inner = i % 4 match {
        case 0 => None
        case 1 => Some(Inner(mutable.ArrayBuffer.empty, mutable.ArrayBuffer.empty)) // {} : the whole field goes
        case 2 => Some(Inner(mutable.ArrayBuffer.empty, mutable.ArrayBuffer(i)))
        case _ => Some(Inner(mutable.ArrayBuffer(i), mutable.ArrayBuffer.empty))
      }
      Entry(i, inner, if (i % 3 == 0) mutable.ArrayBuffer("n") else mutable.ArrayBuffer.empty, i)
    }
    // Some(inner) stays even when all of inner's fields are left out (leaving it out would read back as None)
    val compact = new MPJsons(MPJsonsConfig(compactOptions = true))
    val allEmpty = Entry(1, Some(Inner(mutable.ArrayBuffer.empty, mutable.ArrayBuffer.empty)), mutable.ArrayBuffer.empty, 2)
    compact.serialize(allEmpty) mustBe """{"id":1,"inner":{},"last":2}"""
    m.serialize(allEmpty) mustBe """{"id":1,"inner":{"value":{}},"last":2}"""
    // shifts every boundary by one byte at a time
    for (mpjsons <- List(m, compact); padding <- 0 until 160) {
      val padded = Padded("p" * padding, entries)
      val stream = streamed(padded, mpjsons)
      withClue(s"padding $padding: ") {
        new String(stream.bytes.toByteArray, StandardCharsets.UTF_8) mustBe mpjsons.serialize(padded)
      }
    }
  }

  it must "not hold back a large @omitEmpty value" in {
    val big = Big(mutable.ArrayBuffer.range(0, 300000))
    val stream = streamed(big)
    stream.bytes.toByteArray mustBe m.serializeToBytes(big)
    stream.largestWrite must be <= 128 * 1024
  }

  it must "encode the output of custom converters" in {
    val custom = new MPJsons
    custom.registerConverter[Money](_ => new JsonTypeSerializer[Money] {
      override def serialize(obj: Money, jsonBuilder: StringBuilder): Unit =
        jsonBuilder.append('"').append(obj.cents / 100).append(",").append(obj.cents % 100).append(" zł\"")
    }, _ => null)
    val priced = Priced("ceny", (0L until 20000L).map(i => Money(i * 7)).toList)
    streamed(priced, custom).bytes.toByteArray mustBe custom.serializeToBytes(priced)
  }

  it must "stop at the output limit, having written no more than the limit" in {
    val limit = 200000
    val stream = new RecordingStream
    a[JsonTooLargeException] must be thrownBy m.serializeToStream(document, stream, limit)
    stream.bytes.size must be <= limit
    stream.bytes.size must be > 0 // the chunks before the limit were written
    a[JsonTooLargeException] must be thrownBy new MPJsons(MPJsonsConfig(maxOutputBytes = limit)).serializeToStream(document, new RecordingStream)
  }

  it must "pass on a failure of the stream" in {
    val e = the[Exception] thrownBy m.serializeToStream(document, new RecordingStream(failAfter = 100000))
    Iterator.iterate[Throwable](e)(_.getCause).takeWhile(_ != null).exists(_.isInstanceOf[IOException]) mustBe true
  }

  it must "write a null top-level value as the policy says" in {
    a[IllegalArgumentException] must be thrownBy m.serializeToStream[Line](null, new RecordingStream)
    val stream = new RecordingStream
    new MPJsons(MPJsonsConfig(nullPolicy = NullPolicy.WriteNull)).serializeToStream[Line](null, stream)
    stream.bytes.toString(StandardCharsets.UTF_8) mustBe "null"
  }

  "serializeToByteBuffer" must "wrap the bytes of serializeToBytes" in {
    val expected = m.serializeToBytes(document)
    def bytesOf(buffer: java.nio.ByteBuffer): Array[Byte] = {
      buffer.position() mustBe 0
      val bytes = new Array[Byte](buffer.remaining)
      buffer.get(bytes)
      bytes
    }
    bytesOf(m.serializeToByteBuffer(document)) mustBe expected
    bytesOf(m.serializeToByteBuffer(document, scala.reflect.runtime.universe.typeOf[Document])) mustBe expected
    bytesOf(m.serializeToByteBuffer(document, classOf[Document].getName)) mustBe expected
    bytesOf(m.buildStaticSerializer[Document].serializeToByteBuffer(document)) mustBe expected
    a[JsonTooLargeException] must be thrownBy m.serializeToByteBuffer(document, maxBytes = 1000)
    bytesOf(new MPJsons(MPJsonsConfig(nullPolicy = NullPolicy.WriteNull)).serializeToByteBuffer[Line](null)) mustBe "null".getBytes(StandardCharsets.UTF_8)
  }
}
