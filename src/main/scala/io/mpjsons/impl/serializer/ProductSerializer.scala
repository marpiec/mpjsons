package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.serializer.common.IteratorSerializer
import io.mpjsons.impl.util.TypesUtil

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */

class ProductSerializer(serializerFactory: SerializerFactory, tpe: Type, context: Context)
  extends IteratorSerializer[Any, Product](serializerFactory, tpe, context) {

  override def serialize(obj: Product, jsonBuilder: StringBuilder) {
    serializeIterator(obj.productIterator, jsonBuilder)
  }

}