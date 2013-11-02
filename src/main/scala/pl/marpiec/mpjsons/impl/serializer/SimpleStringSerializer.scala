package pl.marpiec.mpjsons.impl.serializer

import pl.marpiec.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object SimpleStringSerializer extends JsonTypeSerializer {
  def serialize(obj: Any, jsonBuilder: StringBuilder) {
    jsonBuilder.append('"').append(obj).append('"')
  }
}
