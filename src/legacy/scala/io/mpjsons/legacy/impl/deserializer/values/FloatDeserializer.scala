package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractFloatingPointDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object FloatDeserializer extends AbstractFloatingPointDeserializer[Float] {
  override def deserialize(jsonIterator: StringIterator): Float = readFloat(jsonIterator)
}


object JavaFloatDeserializer extends AbstractFloatingPointDeserializer[java.lang.Float] {
  override def deserialize(jsonIterator: StringIterator): java.lang.Float = readFloat(jsonIterator)
}
