package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * Writes a JSON string literal. Runs of characters that need no escaping are appended in bulk;
 * only the characters that need escaping are handled one by one.
 *
 * @param escapeSlash whether "/" is written as "\/" (the historical format) or as-is
 * @author Marcin Pieciukiewicz
 */
class StringSerializerImpl(escapeSlash: Boolean) extends JsonTypeSerializer[Any] with Utf8Writer[Any] {

  /** Escape sequence for each ASCII char that needs one, null for chars that are written as-is. */
  private val escapes: Array[String] = {
    val table = new Array[String](128)
    var i = 0
    while (i < 32) {
      table(i) = "\\u" + StringSerializerImpl.toHex(i.toChar)
      i += 1
    }
    table('\b') = "\\b"
    table('\f') = "\\f"
    table('\n') = "\\n"
    table('\r') = "\\r"
    table('\t') = "\\t"
    table('"') = "\\\""
    table('\\') = "\\\\"
    if (escapeSlash) {
      table('/') = "\\/"
    }
    table
  }

  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    val string = obj.toString
    val length = string.length
    val builder = jsonBuilder.underlying
    if (length >= OutputLimit.LongString) {
      OutputLimit.checkAppend(builder, length)
    }

    builder.append('"')

    var runStart = 0
    var index = 0
    while (index < length) {
      val currentChar = string.charAt(index)
      if (currentChar < 128) {
        val escape = escapes(currentChar)
        if (escape != null) {
          if (currentChar == 0) {
            throw new IllegalArgumentException("Null byte in String is not allowed")
          }
          if (index > runStart) {
            builder.append(string, runStart, index)
          }
          builder.append(escape)
          runStart = index + 1
        }
      }
      index += 1
    }
    if (length > runStart) {
      builder.append(string, runStart, length)
    }

    builder.append('"')
  }

  /** The escape sequences of `escapes`, as bytes (all ASCII). */
  private val escapeBytes: Array[Array[Byte]] = escapes.map(e => if (e == null) null else e.getBytes(java.nio.charset.StandardCharsets.US_ASCII))

  /** Same scan as `serialize`; runs of ASCII are copied in bulk, other runs are encoded. */
  override def writeUtf8(obj: Any, out: Utf8Output): Unit = {
    val string = obj.toString
    val length = string.length

    out.writeByte('"')

    var runStart = 0
    var runIsAscii = true
    var index = 0
    while (index < length) {
      val currentChar = string.charAt(index)
      if (currentChar < 128) {
        val escape = escapeBytes(currentChar)
        if (escape != null) {
          if (currentChar == 0) {
            throw new IllegalArgumentException("Null byte in String is not allowed")
          }
          if (index > runStart) {
            writeRun(out, string, runStart, index, runIsAscii)
          }
          out.writeBytes(escape)
          runStart = index + 1
          runIsAscii = true
        }
      } else {
        runIsAscii = false
      }
      index += 1
    }
    if (length > runStart) {
      writeRun(out, string, runStart, length, runIsAscii)
    }

    out.writeByte('"')
  }

  private def writeRun(out: Utf8Output, string: String, from: Int, to: Int, isAscii: Boolean): Unit = {
    if (isAscii) out.writeAscii(string, from, to) else out.writeText(string, from, to)
  }

  override private[mpjsons] def writesUtf8Itself: Boolean =
    (getClass eq classOf[StringSerializerImpl]) || (this eq StringSerializer)
}

object StringSerializerImpl {
  // To escape control characters from text
  private def toHex(ch: Char): String = {
    val hex = Integer.toHexString(ch)
    val hexLength = hex.length
    if(hexLength < 2) {
      "000"+hex
    } else if(hexLength < 3) {
      "00"+hex
    } else if(hexLength < 4) {
      "0"+hex
    } else {
      hex
    }
  }
}

/** The historical serializer ("/" escaped); `withoutSlashEscape` is the compact variant. */
object StringSerializer extends StringSerializerImpl(true) {
  val withoutSlashEscape: JsonTypeSerializer[Any] = new StringSerializerImpl(false)
}
