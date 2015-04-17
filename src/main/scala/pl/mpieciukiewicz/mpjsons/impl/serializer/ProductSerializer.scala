package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.serializer.common.IteratorSerializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil


/**
 * @author Marcin Pieciukiewicz
 */

case class ProductSerializer[T <: Product](protected val serializerFactory: SerializerFactory) extends IteratorSerializer[T] {

  override def serialize(obj: T, jsonBuilder: StringBuilder) {
    serializeIterator(obj.productIterator, jsonBuilder)
  }

}
