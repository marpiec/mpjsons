package pl.marpiec.mpjsons.impl.deserializer.primitives

import pl.marpiec.mpjsons.impl.deserializer.inner.AbstractIntegerDeserializer

/**
 * @author Marcin Pieciukiewicz
 */
object ByteDeserializer extends AbstractIntegerDeserializer[Byte] {

  protected def toProperInteger(identifier: StringBuilder) = identifier.toByte

}
