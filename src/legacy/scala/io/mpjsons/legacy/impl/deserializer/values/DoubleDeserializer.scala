package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractFloatingPointDeserializer


/**
 * @author Marcin Pieciukiewicz
 */

object DoubleDeserializer extends AbstractFloatingPointDeserializer[Double] {
  override def deserialize(jsonIterator: StringIterator): Double = readDouble(jsonIterator)
}

object JavaDoubleDeserializer extends AbstractFloatingPointDeserializer[java.lang.Double] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Double = readDouble(jsonIterator)
}
