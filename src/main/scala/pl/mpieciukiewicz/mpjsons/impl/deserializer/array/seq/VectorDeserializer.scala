package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.{ClassType, TypesUtil}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object VectorDeserializer extends AbstractJsonArrayDeserializer[Vector[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: ClassType): Vector[_] = buffer.toVector
}
