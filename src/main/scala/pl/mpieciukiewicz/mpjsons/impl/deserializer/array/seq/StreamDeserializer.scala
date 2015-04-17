package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import scala.collection.mutable.ArrayBuffer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object StreamDeserializer extends AbstractJsonArrayDeserializer[Stream[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): Stream[_] = buffer.toStream

}
