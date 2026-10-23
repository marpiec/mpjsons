package io.mpjsons

import java.nio.charset.StandardCharsets.UTF_8

import io.mpjsons.impl.serializer.Utf8Output
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.must.Matchers

import scala.util.Random

object Utf8OutputSpec {

  /** Any char, with many surrogates (paired, unpaired, reversed), control chars and multi-byte chars. */
  def nastyChar(r: Random): Char = r.nextInt(12) match {
    case 0 => (0xD800 + r.nextInt(0x400)).toChar
    case 1 => (0xDC00 + r.nextInt(0x400)).toChar
    case 2 => (0x80 + r.nextInt(0x780)).toChar
    case 3 => (0x800 + r.nextInt(0xD800 - 0x800)).toChar
    case 4 => (0xE000 + r.nextInt(0x2000)).toChar
    case 5 => (1 + r.nextInt(31)).toChar
    case 6 => "\"\\/".charAt(r.nextInt(3))
    case 7 => (0xFF00 + r.nextInt(0x100)).toChar
    case _ => (32 + r.nextInt(95)).toChar
  }

  def nastyString(r: Random): String = {
    val builder = new java.lang.StringBuilder
    val parts = r.nextInt(8)
    for (_ <- 0 until parts) {
      r.nextInt(4) match {
        case 0 => builder.appendCodePoint(0x10000 + r.nextInt(0x100000 - 0x10000))
        case 1 => for (_ <- 0 until r.nextInt(5)) builder.append(nastyChar(r))
        case _ => for (_ <- 0 until r.nextInt(10)) builder.append(('a' + r.nextInt(26)).toChar)
      }
    }
    builder.toString
  }
}

class Utf8OutputSpec extends AnyFlatSpec with Matchers {

  import Utf8OutputSpec._

  private def bytesOf(write: Utf8Output => Unit, capacity: Int = 16): Array[Byte] = {
    val out = new Utf8Output(capacity)
    write(out)
    out.toByteArray
  }

  private def ascii(text: String) = text.getBytes(UTF_8)

  "Utf8Output" must "write ints like StringBuilder.append(int)" in {
    val r = new Random(1)
    val edge = Seq(0, 1, -1, 9, 10, 11, 99, 100, 101, Int.MaxValue, Int.MinValue, Int.MaxValue - 1, Int.MinValue + 1) ++
      (0 to 9).flatMap(e => { val p = math.pow(10, e).toInt; Seq(p - 1, p, p + 1, -p + 1, -p, -p - 1) })
    for (v <- edge ++ Seq.fill(200000)(r.nextInt()) ++ Seq.fill(20000)(r.nextInt(1000) - 500)) {
      bytesOf(_.writeInt(v)) mustBe ascii(new java.lang.StringBuilder().append(v).toString)
    }
  }

  it must "write longs like StringBuilder.append(long)" in {
    val r = new Random(2)
    val edge = Seq(0L, 1L, -1L, Long.MaxValue, Long.MinValue, Long.MaxValue - 1, Long.MinValue + 1,
      Int.MaxValue.toLong, Int.MaxValue + 1L, Int.MinValue.toLong, Int.MinValue - 1L) ++
      (0 to 18).flatMap(e => { val p = BigInt(10).pow(e).toLong; Seq(p - 1, p, p + 1, -p + 1, -p, -p - 1) })
    for (v <- edge ++ Seq.fill(200000)(r.nextLong()) ++ Seq.fill(20000)(r.nextLong() >> r.nextInt(64))) {
      bytesOf(_.writeLong(v)) mustBe ascii(new java.lang.StringBuilder().append(v).toString)
    }
  }

  it must "write doubles and floats like StringBuilder.append" in {
    val r = new Random(3)
    val doubles = Seq(0.0, -0.0, 1.0, -1.0, 0.1, 1e-3, 1e-4, 9.999999e6, 1e7, 1.0e23, Double.MaxValue, Double.MinValue,
      Double.MinPositiveValue, java.lang.Double.MIN_NORMAL, 2.2250738585072014e-308) ++
      Seq.fill(100000)(java.lang.Double.longBitsToDouble(r.nextLong())).filter(d => java.lang.Double.isFinite(d))
    for (v <- doubles) {
      bytesOf(_.writeDouble(v)) mustBe ascii(new java.lang.StringBuilder().append(v).toString)
    }
    val floats = Seq(0.0f, -0.0f, 1.0f, 0.1f, 1e-3f, 1e7f, Float.MaxValue, Float.MinValue, Float.MinPositiveValue) ++
      Seq.fill(100000)(java.lang.Float.intBitsToFloat(r.nextInt())).filter(f => java.lang.Float.isFinite(f))
    for (v <- floats) {
      bytesOf(_.writeFloat(v)) mustBe ascii(new java.lang.StringBuilder().append(v).toString)
    }
  }

  it must "encode text exactly like String.getBytes(UTF_8), including unpaired surrogates" in {
    val r = new Random(4)
    val fixed = Seq("", "a", "ż", "€", "😀", "\uD83D", "\uDE00", "\uDE00\uD83D", "\uD83D😀", "a\uD83Db", "😀\uDE00",
      "\u0080", "߿", "ࠀ", "￿", "퟿", "􏿿", "𐀀")
    for (text <- fixed ++ Seq.fill(50000)(nastyString(r))) {
      withClue(text.map(c => f"\\u${c.toInt}%04x").mkString) {
        bytesOf(_.writeText(text)) mustBe text.getBytes(UTF_8)
      }
    }
  }

  it must "encode a range of a string like the same substring" in {
    val r = new Random(5)
    for (_ <- 0 until 20000) {
      val text = nastyString(r)
      val from = if (text.isEmpty) 0 else r.nextInt(text.length + 1)
      val to = from + r.nextInt(text.length - from + 1)
      bytesOf(_.writeText(text, from, to)) mustBe text.substring(from, to).getBytes(UTF_8)
    }
  }

  it must "copy ASCII text and ranges in bulk" in {
    bytesOf(_.writeAscii("{\"a\":1}")) mustBe ascii("{\"a\":1}")
    bytesOf(_.writeAscii("xyz-abc", 2, 5)) mustBe ascii("z-a")
  }

  it must "grow from a small buffer without losing bytes" in {
    val r = new Random(6)
    val pieces = Seq.fill(20000)(nastyString(r))
    val out = new Utf8Output(1)
    pieces.foreach { p => out.writeText(p); out.writeByte(','); out.writeInt(p.length) }
    out.toByteArray mustBe pieces.map(p => p + "," + p.length).mkString.getBytes(UTF_8)
    out.size mustBe out.toByteArray.length
  }

  it must "encode the output of a serializer that writes into a StringBuilder, reusing one builder" in {
    val r = new Random(7)
    val texts = Seq.fill(5000)(nastyString(r))
    val echo = new JsonTypeSerializer[String] {
      override def serialize(obj: String, jsonBuilder: StringBuilder): Unit = jsonBuilder.append(obj)
    }
    val out = new Utf8Output(16)
    texts.foreach(t => out.writeBridged(echo, t))
    out.toByteArray mustBe texts.map(_.getBytes(UTF_8)).foldLeft(Array.emptyByteArray)(_ ++ _)
  }

  it must "not keep the output of a bridged serializer that failed" in {
    val failing = new JsonTypeSerializer[String] {
      override def serialize(obj: String, jsonBuilder: StringBuilder): Unit = {
        jsonBuilder.append("partial")
        throw new IllegalStateException("failed")
      }
    }
    val echo = new JsonTypeSerializer[String] {
      override def serialize(obj: String, jsonBuilder: StringBuilder): Unit = jsonBuilder.append(obj)
    }
    val out = new Utf8Output(16)
    an[IllegalStateException] must be thrownBy out.writeBridged(failing, "x")
    out.writeBridged(echo, "ok")
    out.toByteArray mustBe ascii("ok")
  }
}
