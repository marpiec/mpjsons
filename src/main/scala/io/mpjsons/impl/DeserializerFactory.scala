package io.mpjsons.impl

import io.mpjsons.impl.factoryimpl.DeserializerFactoryMemoizer

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactory(val ignoreNonExistingFields: Boolean) extends DeserializerFactoryMemoizer(ignoreNonExistingFields)