package pl.marpiec.mpjsons.impl.deserializer.map

import scala.collection.immutable.HashMap
import scala.collection.mutable.ArrayBuffer

/**
 * @author Marcin Pieciukiewicz
 */
object MapDeserializer extends AbstractJsonMapDeserializer[Map[_, _]] {
  protected def toDesiredCollection(keyType: Class[_], valueType: Class[_], buffer: ArrayBuffer[(Any, Any)]) = Map(buffer.toArray:_*)
}
