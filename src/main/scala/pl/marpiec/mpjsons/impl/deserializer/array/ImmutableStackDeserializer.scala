package pl.marpiec.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.collection.immutable.{Stack, Queue}

/**
 * @author Marcin Pieciukiewicz
 */

object ImmutableStackDeserializer extends AbstractJsonArrayDeserializer[Stack[_]] {

  override protected def getSubElementsType(clazz: Class[_], field: Field) = TypesUtil.getSubElementsType(field)

  override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = Stack(buffer.toArray:_*)

}
