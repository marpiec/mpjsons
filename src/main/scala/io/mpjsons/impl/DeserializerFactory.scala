package io.mpjsons.impl

import io.mpjsons.impl.factoryimpl.DeserializerFactoryMemoizer

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactory(ignoreNonExistingFields: Boolean) extends DeserializerFactoryMemoizer(ignoreNonExistingFields)