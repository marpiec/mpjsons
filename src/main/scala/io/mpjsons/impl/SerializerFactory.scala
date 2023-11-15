package io.mpjsons.impl

import io.mpjsons.impl.factoryimpl.SerializerFactoryMemoizer

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactory(ignoreNullFields: Boolean) extends SerializerFactoryMemoizer(ignoreNullFields)