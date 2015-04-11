package pl.mpieciukiewicz.mpjsons.impl.serializer

import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import scala.reflect.runtime.universe._

object SingletonObjectSerializer extends JsonTypeSerializer[AnyRef] {

  override def serialize(obj: AnyRef, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append("{}")
  }
}
