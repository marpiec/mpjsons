package io.mpjsons.impl.serializer

import java.util.Arrays

import io.mpjsons.JsonTypeSerializer

import scala.annotation.nowarn

/**
 * Growable UTF-8 output used by `serializeToBytes`. One instance per call; not thread-safe.
 *
 * Text is encoded the way `String.getBytes(UTF_8)` encodes a whole document: a surrogate pair becomes one 4-byte
 * sequence and an unpaired surrogate becomes '?'. Text is only ever passed in pieces that are delimited by ASCII
 * (quotes, commas, colons, brackets), so encoding piece by piece gives the same bytes as encoding the whole document.
 */
final class Utf8Output(initialCapacity: Int) {

  private var buf: Array[Byte] = new Array[Byte](Math.max(initialCapacity, 16))
  private var count: Int = 0

  /** Formats doubles and floats exactly like StringBuilder.append(double/float); created on first use. */
  private var numberText: java.lang.StringBuilder = _
  /** Output of a serializer that only writes into a StringBuilder (see Utf8Writer.of); created on first use. */
  private var bridgeBuilder: StringBuilder = _
  private var chars: Array[Char] = _

  /** Number of bytes written so far. */
  def size: Int = count

  /** The byte at `index`, which is below `size`. */
  private[serializer] def byteAt(index: Int): Byte = buf(index)

  /** Drops what was written after the first `size` bytes. */
  private[serializer] def truncate(size: Int): Unit = {
    count = size
  }

  def toByteArray: Array[Byte] = Arrays.copyOf(buf, count)

  private def ensure(extra: Int): Unit = {
    if (extra > buf.length - count) {
      grow(extra)
    }
  }

  private def grow(extra: Int): Unit = {
    val needed = count.toLong + extra
    if (needed > Utf8Output.MaxLength) {
      throw new OutOfMemoryError("JSON output larger than " + Utf8Output.MaxLength + " bytes")
    }
    val doubled = buf.length.toLong * 2
    buf = Arrays.copyOf(buf, Math.min(Math.max(doubled, needed), Utf8Output.MaxLength.toLong).toInt)
  }

  def writeByte(b: Byte): Unit = {
    if (count == buf.length) {
      grow(1)
    }
    buf(count) = b
    count += 1
  }

  /** Bytes already encoded as UTF-8 (precomputed field name prefixes and type name wrappers). */
  def writeBytes(bytes: Array[Byte]): Unit = {
    val length = bytes.length
    ensure(length)
    System.arraycopy(bytes, 0, buf, count, length)
    count += length
  }

  /** Text that is known to be ASCII, such as literals and the digits of a number. */
  @nowarn("cat=deprecation")
  def writeAscii(text: String): Unit = {
    val length = text.length
    ensure(length)
    text.getBytes(0, length, buf, count)
    count += length
  }

  /** `text(from until to)`, which must be ASCII: copied in bulk. */
  @nowarn("cat=deprecation")
  def writeAscii(text: String, from: Int, to: Int): Unit = {
    val length = to - from
    ensure(length)
    text.getBytes(from, to, buf, count)
    count += length
  }

  /** Any text. */
  def writeText(text: String): Unit = writeText(text, 0, text.length)

  /** `text(from until to)`, any characters. */
  def writeText(text: String, from: Int, to: Int): Unit = {
    ensure(to - from)
    var index = from
    while (index < to) {
      val c = text.charAt(index)
      if (c < 0x80) {
        if (count == buf.length) {
          grow(to - index)
        }
        buf(count) = c.toByte
        count += 1
        index += 1
      } else {
        index = writeNonAscii(c, text, index, to)
      }
    }
  }

  private def writeText(text: Array[Char], length: Int): Unit = {
    ensure(length)
    var index = 0
    while (index < length) {
      val c = text(index)
      if (c < 0x80) {
        if (count == buf.length) {
          grow(length - index)
        }
        buf(count) = c.toByte
        count += 1
        index += 1
      } else if (c < 0x800) {
        writeTwoBytes(c)
        index += 1
      } else if (Character.isSurrogate(c)) {
        if (Character.isHighSurrogate(c) && index + 1 < length && Character.isLowSurrogate(text(index + 1))) {
          writeCodePoint(Character.toCodePoint(c, text(index + 1)))
          index += 2
        } else {
          writeByte('?')
          index += 1
        }
      } else {
        writeThreeBytes(c)
        index += 1
      }
    }
  }

  /** Writes the non-ASCII char at `index` (with its low surrogate if it is a pair) and returns the next index. */
  private def writeNonAscii(c: Char, text: String, index: Int, to: Int): Int = {
    if (c < 0x800) {
      writeTwoBytes(c)
      index + 1
    } else if (Character.isSurrogate(c)) {
      if (Character.isHighSurrogate(c) && index + 1 < to && Character.isLowSurrogate(text.charAt(index + 1))) {
        writeCodePoint(Character.toCodePoint(c, text.charAt(index + 1)))
        index + 2
      } else {
        // what String.getBytes(UTF_8) writes for an unpaired surrogate
        writeByte('?')
        index + 1
      }
    } else {
      writeThreeBytes(c)
      index + 1
    }
  }

  private def writeTwoBytes(c: Char): Unit = {
    ensure(2)
    buf(count) = (0xC0 | (c >> 6)).toByte
    buf(count + 1) = (0x80 | (c & 0x3F)).toByte
    count += 2
  }

  private def writeThreeBytes(c: Char): Unit = {
    ensure(3)
    buf(count) = (0xE0 | (c >> 12)).toByte
    buf(count + 1) = (0x80 | ((c >> 6) & 0x3F)).toByte
    buf(count + 2) = (0x80 | (c & 0x3F)).toByte
    count += 3
  }

  private def writeCodePoint(codePoint: Int): Unit = {
    ensure(4)
    buf(count) = (0xF0 | (codePoint >> 18)).toByte
    buf(count + 1) = (0x80 | ((codePoint >> 12) & 0x3F)).toByte
    buf(count + 2) = (0x80 | ((codePoint >> 6) & 0x3F)).toByte
    buf(count + 3) = (0x80 | (codePoint & 0x3F)).toByte
    count += 4
  }

  /** Same digits as StringBuilder.append(int). */
  def writeInt(value: Int): Unit = {
    if (value == Int.MinValue) {
      writeAscii("-2147483648")
    } else {
      ensure(11)
      var q = value
      if (q < 0) {
        buf(count) = '-'
        count += 1
        q = -q
      }
      val end = count + Utf8Output.digitCount(q)
      var pos = end
      while (q >= 100) {
        val r = q % 100
        q /= 100
        pos -= 2
        buf(pos) = Utf8Output.DigitTens(r)
        buf(pos + 1) = Utf8Output.DigitOnes(r)
      }
      if (q >= 10) {
        pos -= 2
        buf(pos) = Utf8Output.DigitTens(q)
        buf(pos + 1) = Utf8Output.DigitOnes(q)
      } else {
        pos -= 1
        buf(pos) = ('0' + q).toByte
      }
      count = end
    }
  }

  /** Same digits as StringBuilder.append(long). */
  def writeLong(value: Long): Unit = {
    if (value == Long.MinValue) {
      writeAscii("-9223372036854775808")
    } else if (value >= Int.MinValue && value <= Int.MaxValue) {
      writeInt(value.toInt)
    } else {
      ensure(20)
      var q = value
      if (q < 0) {
        buf(count) = '-'
        count += 1
        q = -q
      }
      val end = count + Utf8Output.digitCount(q)
      var pos = end
      while (q >= 100) {
        val r = (q % 100).toInt
        q /= 100
        pos -= 2
        buf(pos) = Utf8Output.DigitTens(r)
        buf(pos + 1) = Utf8Output.DigitOnes(r)
      }
      if (q >= 10) {
        pos -= 2
        buf(pos) = Utf8Output.DigitTens(q.toInt)
        buf(pos + 1) = Utf8Output.DigitOnes(q.toInt)
      } else {
        pos -= 1
        buf(pos) = ('0' + q).toByte
      }
      count = end
    }
  }

  /** Same text as StringBuilder.append(double). */
  def writeDouble(value: Double): Unit = {
    val text = numberBuilder()
    text.append(value)
    writeNumberText(text)
  }

  /** Same text as StringBuilder.append(float). */
  def writeFloat(value: Float): Unit = {
    val text = numberBuilder()
    text.append(value)
    writeNumberText(text)
  }

  private def numberBuilder(): java.lang.StringBuilder = {
    if (numberText == null) {
      numberText = new java.lang.StringBuilder(32)
    } else {
      numberText.setLength(0)
    }
    numberText
  }

  /**
   * The digits, sign, '.', 'E' and '-' of a formatted number, all ASCII. Measured against copying with getChars and
   * against Double.toString + a bulk copy (both slower; the latter allocates 3x): see performance.md §4d.
   */
  private def writeNumberText(text: java.lang.StringBuilder): Unit = {
    val length = text.length
    ensure(length)
    var index = 0
    while (index < length) {
      buf(count + index) = text.charAt(index).toByte
      index += 1
    }
    count += length
  }

  /**
   * Writes what `serializer` writes into a StringBuilder, encoded as UTF-8. Used for serializers that cannot write
   * bytes themselves, such as custom converters. A bridged serializer only sees a StringBuilder, so no other bridged
   * call can happen on this output while it runs, and one builder is enough.
   */
  def writeBridged[T](serializer: JsonTypeSerializer[T], value: T): Unit = {
    if (bridgeBuilder == null) {
      bridgeBuilder = new StringBuilder(64)
    } else {
      bridgeBuilder.setLength(0)
    }
    serializer.serialize(value, bridgeBuilder)
    val builder = bridgeBuilder.underlying
    val length = builder.length
    if (chars == null || chars.length < length) {
      chars = new Array[Char](Math.max(length, 64))
    }
    builder.getChars(0, length, chars, 0)
    writeText(chars, length)
  }
}

object Utf8Output {

  /** Largest array the JVM reliably allocates. */
  private final val MaxLength = Int.MaxValue - 8

  private val DigitTens: Array[Byte] = Array.tabulate(100)(i => ('0' + i / 10).toByte)
  private val DigitOnes: Array[Byte] = Array.tabulate(100)(i => ('0' + i % 10).toByte)

  private def digitCount(value: Int): Int = {
    var digits = 1
    var limit = 10L
    while (digits < 10 && value >= limit) {
      digits += 1
      limit *= 10
    }
    digits
  }

  private def digitCount(value: Long): Int = {
    var digits = 1
    var limit = 10L
    while (digits < 19 && value >= limit) {
      digits += 1
      limit *= 10
    }
    digits
  }
}

/**
 * Writes a value as UTF-8 JSON, with exactly the text that the corresponding JsonTypeSerializer writes.
 * Implemented by the built-in serializers; not part of the public SPI.
 */
private[mpjsons] trait Utf8Writer[T] {
  def writeUtf8(obj: T, out: Utf8Output): Unit

  /**
   * True when this instance is exactly the class that implements writeUtf8. A subclass (e.g. a custom converter
   * extending a built-in serializer) may change what `serialize` writes, so it is bridged instead.
   */
  private[mpjsons] def writesUtf8Itself: Boolean
}

private[mpjsons] object Utf8Writer {

  /** The serializer itself when it writes UTF-8 directly, otherwise a bridge through a StringBuilder. */
  def of[T](serializer: JsonTypeSerializer[T]): Utf8Writer[T] = serializer match {
    case writer: Utf8Writer[T @unchecked] if writer.writesUtf8Itself => writer
    case other => new Utf8Bridge[T](other)
  }
}

/** Runs a serializer that only writes into a StringBuilder and encodes its output. */
private[mpjsons] final class Utf8Bridge[T](serializer: JsonTypeSerializer[T]) extends Utf8Writer[T] {
  override def writeUtf8(obj: T, out: Utf8Output): Unit = out.writeBridged(serializer, obj)

  override private[mpjsons] def writesUtf8Itself: Boolean = true
}
