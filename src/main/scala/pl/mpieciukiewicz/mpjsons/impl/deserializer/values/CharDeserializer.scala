package pl.mpieciukiewicz.mpjsons.impl.deserializer.values

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractStringDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object CharDeserializer extends AbstractStringDeserializer[Char] {

  override def deserialize(jsonIterator: StringIterator): Char = {

    val deserializedString: String = readString(jsonIterator)

    if (deserializedString.length == 1) {
      deserializedString.charAt(0)
    } else {
      throw new IllegalArgumentException("Char value have to be 1 character long: [" + deserializedString + " ]")
    }
  }

}
