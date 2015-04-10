package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.immutable.Queue
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object QueueDeserializer extends AbstractJsonArrayDeserializer[Queue[_]] {

  override protected def toDesiredCollection[S](elementsType: Class[S], buffer: ArrayBuffer[Any]): Queue[_] = Queue(buffer.toArray:_*)
}
