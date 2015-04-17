package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import scala.collection.mutable.ArrayBuffer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.reflect.runtime.universe._



/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractJsonArrayDeserializer[T] extends JsonTypeDeserializer[T] {


  def deserialize(jsonIterator: StringIterator, tpe: Type)
                 (implicit deserializerFactory: DeserializerFactory): T = {

    jsonIterator.consumeArrayStart()

    val elementsType: Type = getSubElementsType(tpe)

    val buffer = readElementsIntoBuffer(elementsType, jsonIterator)

    toDesiredCollection(buffer, elementsType)

  }

  private def readElementsIntoBuffer(elementsType: Type, jsonIterator: StringIterator)
                                    (implicit deserializerFactory: DeserializerFactory): ArrayBuffer[T] = {
    val buffer = ArrayBuffer[T]()

    val deserializer = deserializerFactory.getDeserializer[T](elementsType)
    
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      
      val value = deserializer.deserialize(jsonIterator, elementsType)
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

  protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): T

}
