package pl.marpiec.mpjsons.impl.serializer

import pl.marpiec.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object SimpleNakedStringSerializer extends JsonTypeSerializer {
  def serialize(obj: Any, jsonBuilder: StringBuilder) {
    jsonBuilder.append(obj)
  }
}
