package pl.marpiec.mpjsons.impl.deserializer.map

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import pl.marpiec.mpjsons.JsonTypeDeserializer
import pl.marpiec.mpjsons.impl.{DeserializerFactory, StringIterator}
import scala.collection.mutable.ArrayBuffer

/**
  * @author Marcin Pieciukiewicz
  */

trait AbstractJsonMapDeserializer[T] extends JsonTypeDeserializer[T] {


   def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): T = {

     jsonIterator.consumeArrayStart()

     val (keyType, valueType) = getDoubleSubElementsType(clazz, field)
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

   private def deserializeValue(jsonIterator: StringIterator, field: Field, valueType: Class[_]): Any = {
     DeserializerFactory.getDeserializer(valueType).deserialize(jsonIterator, valueType, field)
   }

  protected def getDoubleSubElementsType(clazz: Class[_], field: Field) = TypesUtil.getDoubleSubElementsType(field)

  protected def toDesiredCollection(keyType: Class[_], valueType: Class[_], buffer: ArrayBuffer[(Any, Any)]): T


 }
