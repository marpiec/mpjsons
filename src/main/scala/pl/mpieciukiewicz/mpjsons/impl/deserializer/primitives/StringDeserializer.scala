package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.AbstractStringDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object StringDeserializer extends AbstractStringDeserializer[String] {

  override def deserialize(jsonIterator: StringIterator): String = readString(jsonIterator)

}
