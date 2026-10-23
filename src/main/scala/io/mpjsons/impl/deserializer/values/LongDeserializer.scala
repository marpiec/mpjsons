package io.mpjsons.impl.deserializer.values

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object LongDeserializer extends AbstractIntegerDeserializer[Long] {
  override def deserialize(jsonIterator: StringIterator): Long = readLong(jsonIterator)
}

object JavaLongDeserializer extends AbstractIntegerDeserializer[java.lang.Long] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Long = readLong(jsonIterator)
}
