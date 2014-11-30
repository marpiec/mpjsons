package pl.mpieciukiewicz.mpjsons.impl.deserializer.map

import scala.collection.immutable.{ListMap, HashMap}
import scala.collection.mutable.ArrayBuffer

/**
 * @author Marcin Pieciukiewicz
 */
object ListMapDeserializer extends AbstractJsonMapDeserializer[ListMap[_, _]] {
  protected def toDesiredCollection(keyType: Class[_], valueType: Class[_], buffer: ArrayBuffer[(Any, Any)]) = ListMap(buffer.toArray:_*)
}
