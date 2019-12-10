package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.ListMap
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


  protected def readBuffer(jsonIterator: StringIterator): List[(K, V)] = {

    jsonIterator.consumeArrayStart()

    var map: List[(K, V)] = List.empty

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.currentChar != ']') {

      jsonIterator.consumeArrayStart()

      val key = keyDeserializer.deserialize(jsonIterator)

      jsonIterator.consumeArrayValuesSeparator()


      val value = valueDeserializer.deserialize(jsonIterator)

      map ::= (key -> value)


      jsonIterator.consumeArrayEnd() // ]
      jsonIterator.skipWhitespaceChars()

      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }

    }

    jsonIterator.nextChar()
    map
  }


}
