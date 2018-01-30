package io.mpjsons.impl.deserializer.values

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object FloatDeserializer extends AbstractFloatingPointDeserializer[Float] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toFloat
}