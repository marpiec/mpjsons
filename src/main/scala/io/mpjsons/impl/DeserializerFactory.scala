package io.mpjsons.impl

import io.mpjsons.MPJsonsConfig
import io.mpjsons.impl.factoryimpl.DeserializerFactoryMemoizer

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactory(config: MPJsonsConfig) extends DeserializerFactoryMemoizer(config) {

  /** Backward compatible constructor. */
  def this(ignoreNonExistingFields: Boolean) = this(MPJsonsConfig(ignoreNonExistingFields = ignoreNonExistingFields))
}
