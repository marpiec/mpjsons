package pl.mpieciukiewicz.mpjsons.impl.deserializer.values

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.values.IntDeserializer._

/**
 * @author Marcin Pieciukiewicz
 */

object LongDeserializer extends AbstractIntegerDeserializer[Long] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toLong
}