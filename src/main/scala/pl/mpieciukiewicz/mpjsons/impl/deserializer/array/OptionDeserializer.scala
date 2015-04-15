package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import pl.mpieciukiewicz.mpjsons.impl.util.ClassType

import scala.collection.mutable.ArrayBuffer

/**
 * @author Marcin Pieciukiewicz
 */

object OptionDeserializer extends AbstractJsonArrayDeserializer[Option[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: ClassType): Option[Any] = {
    if (buffer.isEmpty) {
      None
    } else {
      Option(buffer.head)
    }
  }
}
