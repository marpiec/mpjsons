package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import scala.collection.mutable.ArrayBuffer
import scala.collection.immutable.Stack
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

object StackDeserializer extends AbstractJsonArrayDeserializer[Stack[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): Stack[_] = Stack(buffer.toArray:_*)
}
