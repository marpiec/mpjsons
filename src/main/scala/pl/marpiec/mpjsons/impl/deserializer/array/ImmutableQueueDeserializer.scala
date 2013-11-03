package pl.marpiec.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.immutable.Queue

/**
 * @author Marcin Pieciukiewicz
 */

object ImmutableQueueDeserializer extends AbstractJsonArrayDeserializer[Queue[_]] {

  override protected def getSubElementsType(clazz: Class[_], field: Field) = TypesUtil.getSubElementsType(field)

  override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = Queue(buffer.toArray:_*)

}
