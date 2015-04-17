package pl.mpieciukiewicz.mpjsons.impl.deserializer.map

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

trait AbstractJsonMapDeserializer[T] extends JsonTypeDeserializer[T] {

  override def deserialize(jsonIterator: StringIterator, tpe: Type)
                          (implicit deserializerFactory: DeserializerFactory): T = {

    jsonIterator.consumeArrayStart()

    val (keyType, valueType) = getDoubleSubElementsType(tpe)
    var mapArray = ArrayBuffer[(Any, Any)]()

    jsonIterator.skipWhitespaceChars()

    while (jsonIterator.currentChar != ']') {

      jsonIterator.consumeArrayStart()

      val key = deserializeValue(jsonIterator, keyType)

      jsonIterator.consumeArrayValuesSeparator()


      val value = deserializeValue(jsonIterator, valueType)

      mapArray.+=((key, value))


      jsonIterator.consumeArrayEnd() // ]
      jsonIterator.skipWhitespaceChars()

      if (jsonIterator.currentChar == ',') {
        jsonIterator.nextChar()
      }

    }

    jsonIterator.nextChar()
    toDesiredCollection(mapArray)
  }

  private def deserializeValue(jsonIterator: StringIterator, tpe: Type)
                              (implicit deserializerFactory: DeserializerFactory): Any = {
    deserializerFactory.getDeserializer(tpe).deserialize(jsonIterator, tpe)
  }

  protected def getDoubleSubElementsType(tpe: Type): (Type, Type) = TypesUtil.getDoubleSubElementsType(tpe)

  protected def toDesiredCollection(buffer: ArrayBuffer[(Any, Any)]): T


}
