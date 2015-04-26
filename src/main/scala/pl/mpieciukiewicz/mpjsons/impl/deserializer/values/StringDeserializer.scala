package pl.mpieciukiewicz.mpjsons.impl.deserializer.values

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractStringDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object StringDeserializer extends AbstractStringDeserializer[String] {

  override def deserialize(jsonIterator: StringIterator): String = readString(jsonIterator)

}
