package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import io.mpjsons.impl.util.TypesUtil

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