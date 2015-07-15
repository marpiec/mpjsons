package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

class StringSerializer() extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder) {

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
        case _ => jsonBuilder.append(currentChar)
      }
    }

    jsonBuilder.append('"')
  }
}
