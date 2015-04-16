package pl.mpieciukiewicz.mpjsons.impl.serializer

import common.IteratorSerializer
import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object IterableSerializer extends JsonTypeSerializer[scala.collection.Iterable[_]] {

  override def serialize(obj: scala.collection.Iterable[_], jsonBuilder: StringBuilder)(implicit serializerFactory: SerializerFactory) = {
    IteratorSerializer.serialize(obj.iterator, jsonBuilder)
  }

}
