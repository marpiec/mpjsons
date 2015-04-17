package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.immutable.HashSet
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object HashSetDeserializer extends AbstractJsonArrayDeserializer[HashSet[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): HashSet[_] = HashSet(buffer.toArray: _*)
}
