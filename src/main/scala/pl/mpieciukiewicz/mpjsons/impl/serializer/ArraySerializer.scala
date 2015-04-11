package pl.mpieciukiewicz.mpjsons.impl.serializer

import common.IteratorSerializer
import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object ArraySerializer extends JsonTypeSerializer[Array[_]] {

  override def serialize(obj: Any, jsonBuilder: StringBuilder) = {
    IteratorSerializer.serialize(obj.asInstanceOf[Array[_]].iterator, jsonBuilder)
  }

}
