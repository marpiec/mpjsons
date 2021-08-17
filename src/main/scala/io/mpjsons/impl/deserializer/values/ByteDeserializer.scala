package io.mpjsons.impl.deserializer.values

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object ByteDeserializer extends AbstractIntegerDeserializer[Byte] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toByte
}

object JavaByteDeserializer extends AbstractIntegerDeserializer[java.lang.Byte] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Byte = readNumberString(jsonIterator).toByte
}
