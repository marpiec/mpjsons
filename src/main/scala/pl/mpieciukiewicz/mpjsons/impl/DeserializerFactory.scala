package pl.mpieciukiewicz.mpjsons.impl

import collection.mutable.ListBuffer

import pl.mpieciukiewicz.mpjsons.impl.deserializer.primitives._
import pl.mpieciukiewicz.mpjsons.impl.deserializer._
import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.{OptionDeserializer, ArrayDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.factoryimpl.{DeserializerFactoryMemoizer, DeserializerFactoryImpl}

/**
 * @author Marcin Pieciukiewicz
 */

class DeserializerFactory extends DeserializerFactoryMemoizer(new DeserializerFactoryImpl)