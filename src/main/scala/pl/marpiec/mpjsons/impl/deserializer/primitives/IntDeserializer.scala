package pl.marpiec.mpjsons.impl.deserializer.primitives

import pl.marpiec.mpjsons.impl.deserializer.inner.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object IntDeserializer extends AbstractIntegerDeserializer[Int] {

  protected def toProperInteger(identifier: StringBuilder) = identifier.toInt

}
