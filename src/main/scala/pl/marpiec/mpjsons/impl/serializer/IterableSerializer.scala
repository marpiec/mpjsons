package pl.marpiec.mpjsons.impl.serializer

import common.IteratorSerializer
import pl.marpiec.mpjsons.JsonTypeSerializer

/**
 * @author Marcin Pieciukiewicz
 */

object IterableSerializer extends JsonTypeSerializer {

  def serialize(obj: Any, jsonBuilder: StringBuilder) = {
    IteratorSerializer.serialize(obj.asInstanceOf[scala.collection.Iterable[_]].iterator, jsonBuilder)
  }

}
