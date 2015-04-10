package pl.mpieciukiewicz.mpjsons.impl.deserializer.map

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import scala.collection.mutable.ArrayBuffer

/**
  * @author Marcin Pieciukiewicz
  */

trait AbstractJsonMapDeserializer[T] extends JsonTypeDeserializer[T] {


   def deserialize(jsonIterator: StringIterator, clazz: Class[T], field: Field): T = {

     jsonIterator.consumeArrayStart()

     val (keyType, valueType): (Class[Any], Class[Any]) = getDoubleSubElementsType(clazz, field)
     var mapArray = ArrayBuffer[(Any, Any)]()

     jsonIterator.skipWhitespaceChars()

     while (jsonIterator.currentChar != ']') {

       jsonIterator.consumeArrayStart()

       val key = deserializeValue(jsonIterator, field, keyType)

       jsonIterator.consumeArrayValuesSeparator()


       val value = deserializeValue(jsonIterator, field, valueType)

       mapArray.+=((key, value))


       jsonIterator.consumeArrayEnd() // ]
       jsonIterator.skipWhitespaceChars()

       if (jsonIterator.currentChar == ',') {
         jsonIterator.nextChar()
       }

     }

     jsonIterator.nextChar()
     toDesiredCollection(keyType, valueType, mapArray)
   }

   private def deserializeValue(jsonIterator: StringIterator, field: Field, valueType: Class[Any]): Any = {
     DeserializerFactory.getDeserializer(valueType).deserialize(jsonIterator, valueType, field)
   }

  protected def getDoubleSubElementsType[A, B](clazz: Class[T], field: Field): (Class[A], Class[B]) = TypesUtil.getDoubleSubElementsType(field)

  protected def toDesiredCollection(keyType: Class[_], valueType: Class[_], buffer: ArrayBuffer[(Any, Any)]): T


 }
