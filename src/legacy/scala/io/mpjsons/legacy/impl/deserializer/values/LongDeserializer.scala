package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object LongDeserializer extends AbstractIntegerDeserializer[Long] {
  override def deserialize(jsonIterator: StringIterator): Long = readLong(jsonIterator)
}

object JavaLongDeserializer extends AbstractIntegerDeserializer[java.lang.Long] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Long = readLong(jsonIterator)
}
