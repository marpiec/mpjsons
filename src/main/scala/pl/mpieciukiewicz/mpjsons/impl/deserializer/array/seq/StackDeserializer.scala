package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.immutable.{Stack, Queue}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object StackDeserializer extends AbstractJsonArrayDeserializer[Stack[_]] {

  override protected def toDesiredCollection[S](elementsType: Class[S], buffer: ArrayBuffer[Any]): Stack[_] = Stack(buffer.toArray:_*)
}
