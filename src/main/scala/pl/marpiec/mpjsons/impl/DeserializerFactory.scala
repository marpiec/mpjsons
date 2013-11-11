package pl.marpiec.mpjsons.impl

import collection.mutable.ListBuffer

import pl.marpiec.mpjsons.impl.deserializer.primitives._
import pl.marpiec.mpjsons.impl.deserializer._
import pl.marpiec.mpjsons.JsonTypeDeserializer
import pl.marpiec.mpjsons.impl.deserializer.array.{OptionDeserializer, ArrayDeserializer}
import pl.marpiec.mpjsons.impl.factoryimpl.{DeserializerFactoryMemoizer, DeserializerFactoryImpl}

/**
 * @author Marcin Pieciukiewicz
 */

object DeserializerFactory extends DeserializerFactoryMemoizer(new DeserializerFactoryImpl)