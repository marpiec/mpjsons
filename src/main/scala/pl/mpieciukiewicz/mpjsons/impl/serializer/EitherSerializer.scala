package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

object EitherSerializer extends JsonTypeSerializer[Either[_,_]] {


  override def serialize(obj: Either[_,_], jsonBuilder: StringBuilder)
                        (implicit serializerFactory: SerializerFactory): Unit = {

    jsonBuilder.append('{')

    val value = obj match {
      case left: Left[_, _] => jsonBuilder.append("\"left\":"); left.left.get
      case right: Right[_, _] => jsonBuilder.append("\"right\":"); right.right.get
    }

    serializerFactory.getSerializer(TypesUtil.getTypeFromClass(value.getClass)).serialize(value, jsonBuilder)

    jsonBuilder.append('}')

  }
}