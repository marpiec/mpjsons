package io.mpjsons.impl.deserializer.values

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object IntDeserializer extends AbstractIntegerDeserializer[Int] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toInt
}

object JavaIntDeserializer extends AbstractIntegerDeserializer[java.lang.Integer] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Integer = readNumberString(jsonIterator).toInt
}