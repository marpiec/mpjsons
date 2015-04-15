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

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: ClassType): Array[_] = {

    if(buffer.isEmpty){
      ObjectConstructionUtil.createArrayInstance[Any](TypesUtil.getClassFromType[Any](elementsType.tpe), 0).asInstanceOf[Array[_]]
    } else {
      val arrayt: Any = ObjectConstructionUtil.createArrayInstance[Any](TypesUtil.getClassFromType[Any](elementsType.tpe), buffer.size)
      val array = arrayt.asInstanceOf[Array[_]]
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

