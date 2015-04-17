package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.immutable.ListSet
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
  * @author Marcin Pieciukiewicz
  */

object ListSetDeserializer extends AbstractJsonArrayDeserializer[ListSet[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): ListSet[_] = ListSet(buffer.toArray:_*)
}
