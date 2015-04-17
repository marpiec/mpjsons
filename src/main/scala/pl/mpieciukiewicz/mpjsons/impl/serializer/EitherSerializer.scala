package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

case class EitherSerializer[L,R](serializerFactory: SerializerFactory) extends JsonTypeSerializer[Either[L,R]] {


  override def serialize(obj: Either[L,R], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('{')

    val value = obj match {
      case left: Left[L,R] => jsonBuilder.append("\"left\":"); left.left.get
      case right: Right[L,R] => jsonBuilder.append("\"right\":"); right.right.get
    }

    serializerFactory.getSerializer(TypesUtil.getTypeFromClass(value.getClass)).serialize(value, jsonBuilder)

    jsonBuilder.append('}')

  }
}