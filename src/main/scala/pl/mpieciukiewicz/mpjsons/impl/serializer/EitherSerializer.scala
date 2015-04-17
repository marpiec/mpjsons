package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.SerializerFactory
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

import scala.reflect.runtime.universe._

class EitherSerializer[L,R](serializerFactory: SerializerFactory, tpe: Type) extends JsonTypeSerializer[Either[L,R]] {

  private val subtypes = TypesUtil.getDoubleSubElementsType(tpe)
  val leftSerializer = serializerFactory.getSerializer(subtypes._1).asInstanceOf[JsonTypeSerializer[L]]
  val rightSerializer = serializerFactory.getSerializer(subtypes._2).asInstanceOf[JsonTypeSerializer[R]]

  override def serialize(obj: Either[L,R], jsonBuilder: StringBuilder): Unit = {

    jsonBuilder.append('{')

    if(obj.isLeft) {
      jsonBuilder.append("\"left\":")
      leftSerializer.serialize(obj.left.get, jsonBuilder)
    } else {
      jsonBuilder.append("\"right\":")
      rightSerializer.serialize(obj.right.get, jsonBuilder)
    }

    jsonBuilder.append('}')

  }
}