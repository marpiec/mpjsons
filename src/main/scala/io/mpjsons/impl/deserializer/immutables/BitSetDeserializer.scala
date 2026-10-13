package io.mpjsons.impl.deserializer.immutables

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.deserializer.values.IntDeserializer
import io.mpjsons.impl.util.Context
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

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
