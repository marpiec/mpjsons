package pl.mpieciukiewicz.mpjsons.impl.deserializer.map

import pl.mpieciukiewicz.mpjsons.impl.util.{ClassType, TypesUtil}
import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import scala.collection.mutable.ArrayBuffer

/**
  * @author Marcin Pieciukiewicz
  */

trait AbstractJsonMapDeserializer[T] extends JsonTypeDeserializer[T] {


   override def deserialize(jsonIterator: StringIterator, classType: ClassType): T = {

     jsonIterator.consumeArrayStart()

     val (keyType, valueType) = getDoubleSubElementsType(classType)
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
     toDesiredCollection(keyType.clazz, valueType.clazz, mapArray)
   }

   private def deserializeValue(jsonIterator: StringIterator, classType: ClassType): Any = {
     DeserializerFactory.getDeserializer(classType.clazz).deserialize(jsonIterator, classType)
   }

  protected def getDoubleSubElementsType(classType: ClassType): (ClassType, ClassType) = TypesUtil.getDoubleSubElementsType(classType.tpe)

  protected def toDesiredCollection(keyType: Class[_], valueType: Class[_], buffer: ArrayBuffer[(Any, Any)]): T


 }
