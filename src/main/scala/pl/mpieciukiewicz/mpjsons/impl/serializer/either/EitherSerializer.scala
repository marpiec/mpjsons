package pl.mpieciukiewicz.mpjsons.impl.serializer.either

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.serializer.BeanSerializer

object LeftSerializer extends JsonTypeSerializer {

  private case class LeftRepresentation(left: Any)

  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    BeanSerializer.serialize(LeftRepresentation(obj.asInstanceOf[Left[Any, Any]].left.get), jsonBuilder)
  }
}


object RightSerializer extends JsonTypeSerializer {

  private case class RightRepresentation(right: Any)

  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    BeanSerializer.serialize(RightRepresentation(obj.asInstanceOf[Right[Any, Any]].right.get), jsonBuilder)
  }
}