package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * Writes a JSON string literal. Runs of characters that need no escaping are appended in bulk;
 * only the characters that need escaping are handled one by one.
 * @author Marcin Pieciukiewicz
 */
object StringSerializer extends JsonTypeSerializer[Any] {

  /** Escape sequence for each ASCII char that needs one, null for chars that are written as-is. */
  private val escapes: Array[String] = {
    val table = new Array[String](128)
    var i = 0
    while (i < 32) {
      table(i) = "\\u" + toHex(i.toChar)
      i += 1
    }
    table('\b') = "\\b"
    table('\f') = "\\f"
    table('\n') = "\\n"
    table('\r') = "\\r"
    table('\t') = "\\t"
    table('"') = "\\\""
    table('\\') = "\\\\"
    table('/') = "\\/"
    table
  }

  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    val string = obj.toString
    val length = string.length
    val builder = jsonBuilder.underlying

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
