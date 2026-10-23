package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object ByteDeserializer extends AbstractIntegerDeserializer[Byte] {
  override def deserialize(jsonIterator: StringIterator): Byte = readByte(jsonIterator)
}

object JavaByteDeserializer extends AbstractIntegerDeserializer[java.lang.Byte] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Byte = readByte(jsonIterator)
}
