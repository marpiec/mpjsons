package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import scala.Any
import pl.mpieciukiewicz.mpjsons.{JsonTypeDeserializer}
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import scala.collection.mutable.ArrayBuffer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil


/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractJsonArrayDeserializer[T] extends JsonTypeDeserializer[T] {


  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): T = {

    jsonIterator.consumeArrayStart()

    val elementsType = getSubElementsType(clazz, field)

    val buffer = readElementsIntoBuffer(elementsType, jsonIterator, field)

    toDesiredCollection(elementsType, buffer)

  }

  private def readElementsIntoBuffer(elementsType: Class[_], jsonIterator: StringIterator, field: Field): ArrayBuffer[Any] = {
    val buffer = ArrayBuffer[Any]()

    val deserializer = DeserializerFactory.getDeserializer(elementsType)
    
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      
      val value = deserializer.deserialize(jsonIterator, elementsType, field)
      buffer += value

      jsonIterator.skipWhitespaceChars()
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }
    }

    jsonIterator.nextChar()
    buffer
  }

  protected def getSubElementsType(clazz: Class[_], field: Field) = TypesUtil.getSubElementsType(field)

  protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]): T

}
