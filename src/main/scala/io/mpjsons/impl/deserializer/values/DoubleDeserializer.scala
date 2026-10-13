package io.mpjsons.impl.deserializer.values

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractFloatingPointDeserializer


/**
 * @author Marcin Pieciukiewicz
 */

object DoubleDeserializer extends AbstractFloatingPointDeserializer[Double] {
  override def deserialize(jsonIterator: StringIterator): Double = readDouble(jsonIterator)
}

object JavaDoubleDeserializer extends AbstractFloatingPointDeserializer[java.lang.Double] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Double = readDouble(jsonIterator)
}
