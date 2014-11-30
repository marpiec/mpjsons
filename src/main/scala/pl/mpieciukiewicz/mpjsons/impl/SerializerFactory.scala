package pl.mpieciukiewicz.mpjsons.impl

import pl.mpieciukiewicz.mpjsons.impl.serializer._
import pl.mpieciukiewicz.mpjsons.JsonTypeSerializer
import pl.mpieciukiewicz.mpjsons.impl.factoryimpl.{SerializerFactoryMemoizer, SerializerFactoryImpl}

/**
 * @author Marcin Pieciukiewicz
 */

object SerializerFactory extends SerializerFactoryMemoizer(new SerializerFactoryImpl)