package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer
import io.mpjsons.impl.SerializerFactory
import io.mpjsons.impl.util.{TypesUtil, Context}

import scala.reflect.runtime.universe._


class OptionSerializer[T](serializerFactory: SerializerFactory, private val tpe: Type, private val context: Context) extends JsonTypeSerializer[Option[T]] {

  val subTypeSerializer = serializerFactory.getSerializer(TypesUtil.getSubElementsType(tpe), context).asInstanceOf[JsonTypeSerializer[T]]

  override def serialize(obj: Option[T], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('{')

    obj match {
      case Some(value) =>
        jsonBuilder.append("\"value\":")
        subTypeSerializer.serialize(value, jsonBuilder)
      case None => ()
    }

    jsonBuilder.append('}')

  }
}
