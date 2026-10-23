package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object IntDeserializer extends AbstractIntegerDeserializer[Int] {
  override def deserialize(jsonIterator: StringIterator): Int = readInt(jsonIterator)
}

object JavaIntDeserializer extends AbstractIntegerDeserializer[java.lang.Integer] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Integer = readInt(jsonIterator)
}
