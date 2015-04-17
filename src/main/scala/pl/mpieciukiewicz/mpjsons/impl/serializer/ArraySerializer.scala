package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.serializer.common.IteratorSerializer

/**
 * @author Marcin Pieciukiewicz
 */

case class ArraySerializer[E, T <: Array[E]](protected val serializerFactory: SerializerFactory) extends IteratorSerializer[Array[E]] {

  override def serialize(obj: Array[E], jsonBuilder: StringBuilder) {
    serializeIterator(obj.iterator, jsonBuilder)
  }

}













