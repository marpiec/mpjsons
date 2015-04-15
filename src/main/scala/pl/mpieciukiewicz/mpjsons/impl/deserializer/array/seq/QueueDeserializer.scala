package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.{ClassType, TypesUtil}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.immutable.Queue
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object QueueDeserializer extends AbstractJsonArrayDeserializer[Queue[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: ClassType): Queue[_] = Queue(buffer.toArray:_*)
}
