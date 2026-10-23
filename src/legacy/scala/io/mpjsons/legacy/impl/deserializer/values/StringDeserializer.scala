package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.jsontypes.AbstractStringDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object StringDeserializer extends AbstractStringDeserializer[String] {

  override def deserialize(jsonIterator: StringIterator): String = readString(jsonIterator)

}
