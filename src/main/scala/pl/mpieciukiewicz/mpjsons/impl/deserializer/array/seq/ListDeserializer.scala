package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object ListDeserializer extends AbstractJsonArrayDeserializer[List[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): List[_] = buffer.toList

}
