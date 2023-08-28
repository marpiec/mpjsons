package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object StringSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {

    val string = obj.toString
    val stringLength = string.length

    var nextIndex = 0

    jsonBuilder.append('"')

    while (stringLength > nextIndex) {
      val currentChar = string.charAt(nextIndex)
      nextIndex = nextIndex + 1

      currentChar match {
        case '"' => jsonBuilder.append("\\\"")
        case '\\' => jsonBuilder.append("\\\\")
        case '/' => jsonBuilder.append("\\/")
        case '\b' => jsonBuilder.append("\\b")
        case '\f' => jsonBuilder.append("\\f")
        case '\n' => jsonBuilder.append("\\n")
        case '\r' => jsonBuilder.append("\\r")
        case '\t' => jsonBuilder.append("\\t")
        case _ if currentChar == 0 => throw new IllegalArgumentException("Null byte in String is not allowed")
        case _ if currentChar < 32 => jsonBuilder.append("\\u"+toHex(currentChar))
        case _ => jsonBuilder.append(currentChar)
      }
    }

    jsonBuilder.append('"')
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
