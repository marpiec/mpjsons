package pl.mpieciukiewicz.mpjsons.impl.serializer

import common.IteratorSerializer
import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object IterableSerializer extends JsonTypeSerializer[scala.collection.Iterable[_]] {

  override def serialize(obj: scala.collection.Iterable[_], jsonBuilder: StringBuilder) = {
    IteratorSerializer.serialize(obj.iterator, jsonBuilder)
  }

}
