package io.mpjsons.impl.deserializer.values

import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.jsontypes.AbstractStringDeserializer

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

object JavaCharDeserializer extends AbstractStringDeserializer[java.lang.Character] {

  override def deserialize(jsonIterator: StringIterator): java.lang.Character = {

    val deserializedString: String = readString(jsonIterator)

    if (deserializedString.length == 1) {
      deserializedString.charAt(0)
    } else {
      throw new IllegalArgumentException("Char value have to be 1 character long: [" + deserializedString + " ]")
    }
  }

}
