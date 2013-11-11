package pl.marpiec.mpjsons.impl.deserializer.array.set

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.marpiec.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object SetDeserializer extends AbstractJsonArrayDeserializer[Set[_]] {

  override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = buffer.toSet

}
