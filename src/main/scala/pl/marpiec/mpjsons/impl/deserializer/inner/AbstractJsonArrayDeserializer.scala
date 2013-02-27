package pl.marpiec.mpjsons.impl.deserializer.inner

import java.lang.reflect.Field
import scala.Any
import pl.marpiec.mpjsons.{StringIterator, JsonTypeDeserializer}
import pl.marpiec.mpjsons.impl.{DeserializerFactory}


/**
 * @author Marcin Pieciukiewicz
 */
trait AbstractJsonArrayDeserializer[T] extends JsonTypeDeserializer[T] {


  def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): T = {

    jsonIterator.consumeArrayStart

    val elementsType = getSubElementsType(clazz, field)

    val listInstance = readElementsIntoList(elementsType, jsonIterator, field)

    if (listInstance.isEmpty) {
      createEmptyCollectionInstance(elementsType)
    } else {
      convertListIntoCollectionAndReturn(elementsType, listInstance)
    }

  }

  private def readElementsIntoList(elementsType: Class[_], jsonIterator: StringIterator, field: Field): List[Any] = {
    var listInstance = List[Any]()

    jsonIterator.skipWhitespaceChars
    while (jsonIterator.currentChar != ']') {
      val deserializer = DeserializerFactory.getDeserializer(elementsType)
      val value = deserializer.deserialize(jsonIterator, elementsType, field)
      listInstance = value :: listInstance

      jsonIterator.skipWhitespaceChars
      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar
      }
    }

    jsonIterator.nextChar
    listInstance
  }

  protected def getSubElementsType(clazz: Class[_], field: Field): Class[_]

  protected def createEmptyCollectionInstance(elementsType: Class[_]): T

  protected def convertListIntoCollectionAndReturn(elementsType: Class[_], listInstance: List[Any]): T

}
