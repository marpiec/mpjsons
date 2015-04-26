package pl.mpieciukiewicz.mpjsons.impl.deserializer.values

import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.deserializer.jsontypes.AbstractIntegerDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.values.FloatDeserializer._

/**
 * @author Marcin Pieciukiewicz
 */

object IntDeserializer extends AbstractIntegerDeserializer[Int] {
  override def deserialize(jsonIterator: StringIterator) = readNumberString(jsonIterator).toInt
}