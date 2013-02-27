package pl.marpiec.mpjsons.impl.deserializer.primitives

import pl.marpiec.mpjsons.impl.deserializer.inner.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object ShortDeserializer extends AbstractIntegerDeserializer[Short] {

  protected def toProperInteger(identifier: StringBuilder) = identifier.toShort

}
