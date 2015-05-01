package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.TypesUtil
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.Map
import scala.collection.mutable
import scala.reflect.runtime.universe._
import io.mpjsons.impl.util.Context
/**
 * @author Marcin Pieciukiewicz
 */

abstract class AbstractJsonMapDeserializer[K, V, M]
(private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
  extends JsonTypeDeserializer[M] {

  val (keyType, valueType): (Type, Type) = TypesUtil.getDoubleSubElementsType(tpe)
  val keyDeserializer = deserializerFactory.getDeserializer[K](keyType, context)
  val valueDeserializer = deserializerFactory.getDeserializer[V](valueType, context)


  protected def readBuffer(jsonIterator: StringIterator): mutable.ListMap[K, V] = {

    jsonIterator.consumeArrayStart()

    var map = mutable.ListMap[K, V]()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.currentChar != ']') {

      jsonIterator.consumeArrayStart()

      val key = keyDeserializer.deserialize(jsonIterator)

      jsonIterator.consumeArrayValuesSeparator()


      val value = valueDeserializer.deserialize(jsonIterator)

      map += key -> value


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
