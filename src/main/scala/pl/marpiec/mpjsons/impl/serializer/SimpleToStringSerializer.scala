package pl.marpiec.mpjsons.impl.serializer

import pl.marpiec.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object SimpleToStringSerializer extends JsonTypeSerializer {
  def serialize(obj: Any, jsonBuilder: StringBuilder) {
    jsonBuilder.append(obj)
  }
}
