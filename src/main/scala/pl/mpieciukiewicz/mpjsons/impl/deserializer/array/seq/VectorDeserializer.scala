package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import scala.collection.mutable.ArrayBuffer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

object VectorDeserializer extends AbstractJsonArrayDeserializer[Vector[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): Vector[_] = buffer.toVector
}
