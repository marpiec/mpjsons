package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import scala.collection.mutable.ArrayBuffer

/**
 * @author Marcin Pieciukiewicz
 */

object OptionDeserializer extends AbstractJsonArrayDeserializer[Option[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[Any]): Option[Any] = {
    if (buffer.isEmpty) {
      None
    } else {
      Option(buffer.head)
    }
  }
}
