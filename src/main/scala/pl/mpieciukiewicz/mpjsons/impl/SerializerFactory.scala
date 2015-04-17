package pl.mpieciukiewicz.mpjsons.impl

import pl.mpieciukiewicz.mpjsons.impl.factoryimpl.{SerializerFactoryImpl, SerializerFactoryMemoizer}

/**
 * @author Marcin Pieciukiewicz
 */

class SerializerFactory extends SerializerFactoryMemoizer(new SerializerFactoryImpl)