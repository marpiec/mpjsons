package pl.mpieciukiewicz.mpjsons.impl.deserializer.values

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object FloatDeserializer extends AbstractIntegerDeserializer[Float] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toFloat
}