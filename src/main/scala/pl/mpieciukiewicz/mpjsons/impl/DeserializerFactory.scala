package io.mpjsons.impl

import io.mpjsons.impl.factoryimpl.{DeserializerFactoryImpl, DeserializerFactoryMemoizer}

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactory extends DeserializerFactoryMemoizer(new DeserializerFactoryImpl)