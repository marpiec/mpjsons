package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.inner.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object ByteDeserializer extends AbstractIntegerDeserializer[Byte] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toByte
}
