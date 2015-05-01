package io.mpjsons.impl.deserializer.values

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractStringDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object StringDeserializer extends AbstractStringDeserializer[String] {

  override def deserialize(jsonIterator: StringIterator): String = readString(jsonIterator)

}
