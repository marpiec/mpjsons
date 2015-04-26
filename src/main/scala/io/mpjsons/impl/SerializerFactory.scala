package io.mpjsons.impl

import io.mpjsons.impl.factoryimpl.{SerializerFactoryImpl, SerializerFactoryMemoizer}

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactory extends SerializerFactoryMemoizer(new SerializerFactoryImpl)