package pl.mpieciukiewicz.mpjsons.impl.deserializer.map

import scala.collection.mutable.ArrayBuffer

/**
 * @author Marcin Pieciukiewicz
 */
object MapDeserializer extends AbstractJsonMapDeserializer[Map[_, _]] {
  protected def toDesiredCollection(buffer: ArrayBuffer[(Any, Any)]) = Map(buffer.toArray: _*)
}
