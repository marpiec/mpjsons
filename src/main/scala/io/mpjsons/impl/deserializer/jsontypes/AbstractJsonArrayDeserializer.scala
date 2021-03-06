package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
abstract class AbstractJsonArrayDeserializer[T, C]
(private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
  extends JsonTypeDeserializer[C] {

  val elementsType: Type = getSubElementsType(tpe)
  val deserializer = deserializerFactory.getDeserializer[T](elementsType, context)

  protected def deserializeArray(jsonIterator: StringIterator, tpe: Type): ArrayBuffer[T] = {

    jsonIterator.consumeArrayStart()

    val buffer = ArrayBuffer[T]()

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
    buffer
  }


  protected def getSubElementsType[S](tpe: Type): Type = TypesUtil.getSubElementsType(tpe)

}
