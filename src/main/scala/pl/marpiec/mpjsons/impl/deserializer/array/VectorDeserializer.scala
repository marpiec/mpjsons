package pl.marpiec.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}

/**
 * @author Marcin Pieciukiewicz
 */

object VectorDeserializer extends AbstractJsonArrayDeserializer[Vector[_]] {

  override protected def getSubElementsType(clazz: Class[_], field: Field) = TypesUtil.getSubElementsType(field)

  override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = buffer.toVector

}
