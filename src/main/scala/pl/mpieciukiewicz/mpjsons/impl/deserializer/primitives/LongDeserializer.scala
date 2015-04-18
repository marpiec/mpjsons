package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.inner.AbstractIntegerDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives.IntDeserializer._

/**
 * @author Marcin Pieciukiewicz
 */

object LongDeserializer extends AbstractIntegerDeserializer[Long] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toLong
}