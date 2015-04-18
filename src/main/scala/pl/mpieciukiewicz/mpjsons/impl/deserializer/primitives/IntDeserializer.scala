package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.inner.AbstractIntegerDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives.FloatDeserializer._

/**
 * @author Marcin Pieciukiewicz
 */

object IntDeserializer extends AbstractIntegerDeserializer[Int] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toInt
}