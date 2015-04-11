package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.mutable.ArrayBuffer

/**
 * @author Marcin Pieciukiewicz
 */

object ListDeserializer extends AbstractJsonArrayDeserializer[List[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[Any]): List[_] = buffer.toList

}
