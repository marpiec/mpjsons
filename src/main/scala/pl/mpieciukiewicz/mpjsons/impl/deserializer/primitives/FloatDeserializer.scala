package pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives

import pl.mpieciukiewicz.mpjsons.impl.deserializer.inner.AbstractFloatingPointDeserializer


/**
 * @author Marcin Pieciukiewicz
 */

object FloatDeserializer extends AbstractFloatingPointDeserializer[Float] {

  protected def toProperFloatingPoint(identifier: StringBuilder) = identifier.toFloat

}