package pl.marpiec.mpjsons.impl

import pl.marpiec.mpjsons.impl.serializer._
import pl.marpiec.mpjsons.JsonTypeSerializer
import pl.marpiec.mpjsons.impl.factoryimpl.{SerializerFactoryMemoizer, SerializerFactoryImpl}

/**
 * @author Marcin Pieciukiewicz
 */

object SerializerFactory extends SerializerFactoryMemoizer(new SerializerFactoryImpl)