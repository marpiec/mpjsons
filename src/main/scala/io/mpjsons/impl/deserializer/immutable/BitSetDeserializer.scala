package io.mpjsons.impl.deserializer.immutable

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.BitSet
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BitSetDeserializer(deserializerFactory: DeserializerFactory, tpe: Type)
  extends JsonTypeDeserializer[BitSet] {

  val deserializer = deserializerFactory.getDeserializer[Int](typeOf[Int])


  override def deserialize(jsonIterator: StringIterator): BitSet = {

    jsonIterator.consumeArrayStart()

    val buffer = ArrayBuffer[Int]()

    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {

      val value = deserializer.deserialize(jsonIterator)
      buffer += value

      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }
    }

    jsonIterator.nextChar()
    BitSet(buffer.toArray: _*)
  }

}
