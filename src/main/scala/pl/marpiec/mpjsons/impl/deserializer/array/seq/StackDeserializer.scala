package pl.marpiec.mpjsons.impl.deserializer.array.seq

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.immutable.{Stack, Queue}
import pl.marpiec.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object StackDeserializer extends AbstractJsonArrayDeserializer[Stack[_]] {

  override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = Stack(buffer.toArray:_*)

}
