package io.mpjsons.impl

import io.mpjsons.{MPJsonsConfig, NullPolicy}
import io.mpjsons.impl.factoryimpl.SerializerFactoryMemoizer

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactory(config: MPJsonsConfig) extends SerializerFactoryMemoizer(config) {

  def this(nullPolicy: NullPolicy) = this(MPJsonsConfig(nullPolicy = nullPolicy))

  /** Backward compatible constructor: `ignoreNullFields = true` omits null fields, false fails on them. */
  def this(ignoreNullFields: Boolean) = this(if (ignoreNullFields) NullPolicy.Omit else NullPolicy.Fail)
}
