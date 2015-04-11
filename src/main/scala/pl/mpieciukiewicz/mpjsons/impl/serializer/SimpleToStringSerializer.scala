package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object SimpleToStringSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder) {
    jsonBuilder.append(obj)
  }
}
