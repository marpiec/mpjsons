package pl.mpieciukiewicz.mpjsons.impl.deserializer.map

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
 * @author Marcin Pieciukiewicz
 */
object HashMapDeserializer extends AbstractJsonMapDeserializer[HashMap[_, _]] {
  protected def toDesiredCollection(buffer: ArrayBuffer[(Any, Any)]) = HashMap(buffer.toArray: _*)
}
