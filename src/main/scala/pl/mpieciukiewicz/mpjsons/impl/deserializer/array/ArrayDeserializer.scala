package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.{ClassType, TypesUtil, ObjectConstructionUtil}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object ArrayDeserializer extends AbstractJsonArrayDeserializer[Array[_]] {

  override protected def getSubElementsType[S](classType: ClassType) = TypesUtil.getArraySubElementsType(classType)

  override protected def toDesiredCollection(buffer: ArrayBuffer[Any]): Array[Any] = {
    buffer.toArray
//    if(buffer.isEmpty){
//      ObjectConstructionUtil.createArrayInstance(elementsType, 0)
//    } else {
//      val array = ObjectConstructionUtil.createArrayInstance(elementsType, buffer.size)
//      var list = buffer.toList
//      var p = 0
//
//      while (list.nonEmpty) {
//        java.lang.reflect.Array.set(array, p, list.head)
//        list = list.tail
//        p = p + 1
//      }
//      array
//    }


  }



}

