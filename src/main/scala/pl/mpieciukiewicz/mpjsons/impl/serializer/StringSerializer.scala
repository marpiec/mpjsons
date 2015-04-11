package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object StringSerializer extends JsonTypeSerializer[String] {
  override def serialize(string: String, jsonBuilder: StringBuilder) {

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
