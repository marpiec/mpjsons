package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.util.{Context, MapKeys, NullValues, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * Reads both map forms: the historical array of [key,value] pairs and the object form {"key":value,...}
 * (the object form only for the scalar key types listed in MapKeys).
 * @author Marcin Pieciukiewicz
 */

abstract class AbstractJsonMapDeserializer[K, V, M]
(private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
  extends JsonTypeDeserializer[M] {

  val (keyType, valueType): (Type, Type) = TypesUtil.getDoubleSubElementsType(tpe)
  val keyDeserializer = deserializerFactory.getDeserializer[K](keyType, context)
  val valueDeserializer = deserializerFactory.getDeserializer[V](valueType, context)
  private val keyNull = NullValues.forType(keyType, deserializerFactory.config)
  private val valueNull = NullValues.forType(valueType, deserializerFactory.config)
  private val keyParser: String => Any = MapKeys.parser(keyType).orNull


  /** Reads the map straight into the given builder, in input order. */
  protected def readInto[R](jsonIterator: StringIterator, builder: mutable.Builder[(K, V), R]): R = {
    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == '{') readObjectInto(jsonIterator, builder) else readPairsInto(jsonIterator, builder)
  }

  private def readPairsInto[R](jsonIterator: StringIterator, builder: mutable.Builder[(K, V), R]): R = {

    jsonIterator.consumeArrayStart()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.currentChar != ']') {

      jsonIterator.consumeArrayStart()

      val key = NullValues.read(jsonIterator, keyNull, keyDeserializer)

      jsonIterator.consumeArrayValuesSeparator()

      val value = NullValues.read(jsonIterator, valueNull, valueDeserializer)

      builder += ((key, value))

      jsonIterator.consumeArrayEnd() // ]
      jsonIterator.afterValue(']')

    }

    jsonIterator.nextChar()
    builder.result()
  }

  private def readObjectInto[R](jsonIterator: StringIterator, builder: mutable.Builder[(K, V), R]): R = {

    if (keyParser == null) {
      throw new IllegalArgumentException("A map with key type " + keyType + " cannot be read from a JSON object; expected an array of [key,value] pairs")
    }

    jsonIterator.consumeObjectStart()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.currentChar != '}') {

      val key = keyParser(IdentifierDeserializer.deserialize(jsonIterator)).asInstanceOf[K]

      jsonIterator.consumeFieldValueSeparator()

      val value = NullValues.read(jsonIterator, valueNull, valueDeserializer)

      builder += ((key, value))

      jsonIterator.afterValue('}')
    }

    jsonIterator.nextChar()
    builder.result()
  }

  /** Pairs in reverse input order (kept for compatibility with earlier subclasses). */
  protected def readBuffer(jsonIterator: StringIterator): List[(K, V)] = {
    readInto(jsonIterator, List.newBuilder[(K, V)]).reverse
  }


}
