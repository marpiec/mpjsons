package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.inner.{AbstractIntegerDeserializer, AbstractFloatingPointDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives.DoubleDeserializer._


/**
 * @author Marcin Pieciukiewicz
 */

object FloatDeserializer extends AbstractIntegerDeserializer[Float] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toFloat
}