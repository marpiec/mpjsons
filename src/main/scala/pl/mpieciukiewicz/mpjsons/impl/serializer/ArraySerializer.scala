package pl.mpieciukiewicz.mpjsons.impl.serializer

import common.IteratorSerializer
import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object ArraySerializer extends JsonTypeSerializer {

  def serialize(obj: Any, jsonBuilder: StringBuilder) = {
    IteratorSerializer.serialize(obj.asInstanceOf[Array[_]].iterator, jsonBuilder)
  }

}