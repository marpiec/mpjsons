package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.{TypesUtil, ObjectConstructionUtil}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}


/**
 * @author Marcin Pieciukiewicz
 */

object ArrayDeserializer extends AbstractJsonArrayDeserializer[Array[_]] {

  override protected def getSubElementsType[S](clazz: Class[Array[_]], field: Field): Class[S] = TypesUtil.getArraySubElementsType(clazz).asInstanceOf[Class[S]]

  override protected def toDesiredCollection[S](elementsType: Class[S], buffer: ArrayBuffer[Any]): Array[_] = {

    if(buffer.isEmpty){
      ObjectConstructionUtil.createArrayInstance(elementsType, 0)
    } else {
      val array = ObjectConstructionUtil.createArrayInstance(elementsType, buffer.size)
      var list = buffer.toList
      var p = 0

      while (list.nonEmpty) {
        java.lang.reflect.Array.set(array, p, list.head)
        list = list.tail
        p = p + 1
      }
      array
    }


  }



}

