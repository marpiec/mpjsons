package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.serializer.common.IteratorSerializer

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ArraySerializer[E](serializerFactory: SerializerFactory, tpe: Type)
  extends IteratorSerializer[E, Array[E]](serializerFactory, tpe) {

  override def serialize(obj: Array[E], jsonBuilder: StringBuilder) {
    serializeIterator(obj.iterator, jsonBuilder)
  }

}













