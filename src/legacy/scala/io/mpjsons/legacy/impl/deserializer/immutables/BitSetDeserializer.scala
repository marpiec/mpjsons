package io.mpjsons.legacy.impl.deserializer.immutables

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.deserializer.values.IntDeserializer
import io.mpjsons.legacy.impl.util.Context
import io.mpjsons.legacy.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.BitSet
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BitSetDeserializer(deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends JsonTypeDeserializer[BitSet] {

  private val deserializer = deserializerFactory.getDeserializer[Int](typeOf[Int], context)
  private val useBuiltIn = deserializer eq IntDeserializer


  override def deserialize(jsonIterator: StringIterator): BitSet = {

    jsonIterator.consumeArrayStart()

    val builder = BitSet.newBuilder

    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {

      builder += (if (useBuiltIn) IntDeserializer.deserialize(jsonIterator) else deserializer.deserialize(jsonIterator))

      jsonIterator.afterValue(']')
    }

    jsonIterator.nextChar()
    builder.result()
  }

}
