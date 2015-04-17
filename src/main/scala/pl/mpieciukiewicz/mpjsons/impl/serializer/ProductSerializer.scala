package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.serializer.common.IteratorSerializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

class ProductSerializer(serializerFactory: SerializerFactory, tpe: Type)
  extends IteratorSerializer[Any, Product](serializerFactory, tpe) {

  override def serialize(obj: Product, jsonBuilder: StringBuilder) {
    serializeIterator(obj.productIterator, jsonBuilder)
  }

}