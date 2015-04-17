package pl.mpieciukiewicz.mpjsons.impl

import pl.mpieciukiewicz.mpjsons.impl.factoryimpl.{DeserializerFactoryImpl, DeserializerFactoryMemoizer}

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactory extends DeserializerFactoryMemoizer(new DeserializerFactoryImpl)