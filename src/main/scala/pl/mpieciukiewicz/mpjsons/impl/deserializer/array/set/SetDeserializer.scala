package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object SetDeserializer extends AbstractJsonArrayDeserializer[Set[Any]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): Set[Any] = buffer.toSet
}
