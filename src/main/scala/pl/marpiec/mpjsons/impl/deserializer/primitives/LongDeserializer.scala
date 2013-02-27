package pl.marpiec.mpjsons.impl.deserializer.primitives

import pl.marpiec.mpjsons.impl.deserializer.inner.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object LongDeserializer extends AbstractIntegerDeserializer[Long] {

  protected def toProperInteger(identifier: StringBuilder) = identifier.toLong

}
