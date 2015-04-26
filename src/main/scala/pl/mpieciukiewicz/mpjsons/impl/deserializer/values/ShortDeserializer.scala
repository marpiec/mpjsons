package pl.mpieciukiewicz.mpjsons.impl.deserializer.values

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.values.LongDeserializer._

/**
 * @author Marcin Pieciukiewicz
 */

object ShortDeserializer extends AbstractIntegerDeserializer[Short] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toShort
}