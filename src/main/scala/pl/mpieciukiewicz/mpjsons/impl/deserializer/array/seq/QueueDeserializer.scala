package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.immutable.Queue
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object QueueDeserializer extends AbstractJsonArrayDeserializer[Queue[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): Queue[_] = Queue(buffer.toArray: _*)
}
