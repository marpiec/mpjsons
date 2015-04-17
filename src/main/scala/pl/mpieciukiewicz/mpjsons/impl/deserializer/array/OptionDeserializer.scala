package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object OptionDeserializer extends AbstractJsonArrayDeserializer[Option[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): Option[Any] = {
    if (buffer.isEmpty) {
      None
    } else {
      Option(buffer.head)
    }
  }
}
