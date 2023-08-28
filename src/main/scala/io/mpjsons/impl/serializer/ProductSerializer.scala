package io.mpjsons.impl.serializer

import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import io.mpjsons.impl.util.Context

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ProductSerializer(serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends IteratorSerializer[Any, Product](serializerFactory, tpe, context) {

  override def serialize(obj: Product, jsonBuilder: StringBuilder): Unit = {
    serializeIterator(obj.productIterator, jsonBuilder)
  }

}