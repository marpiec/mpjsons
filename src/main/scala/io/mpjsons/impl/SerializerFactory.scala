package io.mpjsons.impl

import io.mpjsons.NullPolicy
import io.mpjsons.impl.factoryimpl.SerializerFactoryMemoizer

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactory(nullPolicy: NullPolicy) extends SerializerFactoryMemoizer(nullPolicy) {

  /** Backward compatible constructor: `ignoreNullFields = true` omits null fields, false fails on them. */
  def this(ignoreNullFields: Boolean) = this(if (ignoreNullFields) NullPolicy.Omit else NullPolicy.Fail)
}
