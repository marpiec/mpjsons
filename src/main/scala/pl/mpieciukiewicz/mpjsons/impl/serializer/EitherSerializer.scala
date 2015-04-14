package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory

object EitherSerializer extends JsonTypeSerializer {

  private case class LeftRepresentation(left: Any)

  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('{')

    val value = obj match {
      case left: Left[_, _] => jsonBuilder.append("\"left\":"); left.left.get
      case right: Right[_, _] => jsonBuilder.append("\"right\":"); right.right.get
    }

    SerializerFactory.getSerializer(value.getClass).serialize(value, jsonBuilder)

    jsonBuilder.append('}')

  }
}