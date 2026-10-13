package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

abstract class AbstractJsonMapDeserializer[K, V, M]
(private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
  extends JsonTypeDeserializer[M] {

  val (keyType, valueType): (Type, Type) = TypesUtil.getDoubleSubElementsType(tpe)
  val keyDeserializer = deserializerFactory.getDeserializer[K](keyType, context)
  val valueDeserializer = deserializerFactory.getDeserializer[V](valueType, context)


  /** Reads the [[k,v],...] pair array straight into the given builder, in input order. */
  protected def readInto[R](jsonIterator: StringIterator, builder: mutable.Builder[(K, V), R]): R = {

    jsonIterator.consumeArrayStart()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.currentChar != ']') {

      jsonIterator.consumeArrayStart()

      val key = keyDeserializer.deserialize(jsonIterator)

      jsonIterator.consumeArrayValuesSeparator()

      val value = valueDeserializer.deserialize(jsonIterator)

      builder += ((key, value))

      jsonIterator.consumeArrayEnd() // ]
      jsonIterator.skipWhitespaceChars()

      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
        jsonIterator.skipWhitespaceChars()
      }

    }

    jsonIterator.nextChar()
    builder.result()
  }

  /** Pairs in reverse input order (kept for compatibility with earlier subclasses). */
  protected def readBuffer(jsonIterator: StringIterator): List[(K, V)] = {
    readInto(jsonIterator, List.newBuilder[(K, V)]).reverse
  }


}
