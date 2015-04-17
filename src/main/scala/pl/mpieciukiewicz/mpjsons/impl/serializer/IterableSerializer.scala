package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.serializer.common.IteratorSerializer

/**
 * @author Marcin Pieciukiewicz
 */

case class IterableSerializer[E, T <: Iterable[E]](protected val serializerFactory: SerializerFactory) extends IteratorSerializer[Iterable[E]] {

  override def serialize(obj: Iterable[E], jsonBuilder: StringBuilder) {
    serializeIterator(obj.iterator, jsonBuilder)
  }

}






