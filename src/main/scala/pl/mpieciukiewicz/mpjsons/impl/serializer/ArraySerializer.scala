package pl.mpieciukiewicz.mpjsons.impl.serializer

import common.IteratorSerializer
import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
/**
 * @author Marcin Pieciukiewicz
 */

object ArraySerializer extends JsonTypeSerializer[Array[_]] {

  override def serialize(obj: Array[_], jsonBuilder: StringBuilder)
                        (implicit serializerFactory: SerializerFactory) = {
    IteratorSerializer.serialize(obj.iterator, jsonBuilder)
  }

}













