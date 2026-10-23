package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object ShortDeserializer extends AbstractIntegerDeserializer[Short] {
  override def deserialize(jsonIterator: StringIterator): Short = readShort(jsonIterator)
}

object JavaShortDeserializer extends AbstractIntegerDeserializer[java.lang.Short] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Short = readShort(jsonIterator)
}
