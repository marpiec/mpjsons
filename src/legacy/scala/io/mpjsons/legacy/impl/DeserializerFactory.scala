package io.mpjsons.legacy.impl

import io.mpjsons.legacy.MPJsonsConfig
import io.mpjsons.legacy.impl.factoryimpl.DeserializerFactoryMemoizer

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactory(config: MPJsonsConfig) extends DeserializerFactoryMemoizer(config) {

  /** Backward compatible constructor. */
  def this(ignoreNonExistingFields: Boolean) = this(MPJsonsConfig(ignoreNonExistingFields = ignoreNonExistingFields))
}
