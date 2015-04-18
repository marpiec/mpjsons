package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.inner.AbstractIntegerDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives.LongDeserializer._

/**
 * @author Marcin Pieciukiewicz
 */

object ShortDeserializer extends AbstractIntegerDeserializer[Short] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toShort
}