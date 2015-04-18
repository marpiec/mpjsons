package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.inner.{AbstractIntegerDeserializer, AbstractFloatingPointDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives.ByteDeserializer._


/**
 * @author Marcin Pieciukiewicz
 */

object DoubleDeserializer extends AbstractIntegerDeserializer[Double] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toDouble
}