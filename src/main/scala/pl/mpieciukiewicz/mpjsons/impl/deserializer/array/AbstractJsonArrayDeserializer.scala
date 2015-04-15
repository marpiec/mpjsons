package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import scala.Any
import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import scala.collection.mutable.ArrayBuffer
import pl.mpieciukiewicz.mpjsons.impl.util.{ClassType, TypesUtil}
import scala.reflect.runtime.universe._



/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractJsonArrayDeserializer[T] extends JsonTypeDeserializer[T] {


  def deserialize(jsonIterator: StringIterator, classType: ClassType): T = {

    jsonIterator.consumeArrayStart()

    val elementsType: ClassType = getSubElementsType(classType)

    val buffer = readElementsIntoBuffer(elementsType, jsonIterator)

    toDesiredCollection(buffer, elementsType)

  }

  private def readElementsIntoBuffer(elementsType: ClassType, jsonIterator: StringIterator): ArrayBuffer[T] = {
    val buffer = ArrayBuffer[T]()

    val deserializer = DeserializerFactory.getDeserializer[T](elementsType.tpe)
    
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

  protected def getSubElementsType[S](classType: ClassType): ClassType = TypesUtil.getSubElementsType(classType.tpe)

  protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: ClassType): T

}
