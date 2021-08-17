package io.mpjsons.impl.deserializer.values

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object ShortDeserializer extends AbstractIntegerDeserializer[Short] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toShort
}

object JavaShortDeserializer extends AbstractIntegerDeserializer[java.lang.Short] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Short = readNumberString(jsonIterator).toShort
}