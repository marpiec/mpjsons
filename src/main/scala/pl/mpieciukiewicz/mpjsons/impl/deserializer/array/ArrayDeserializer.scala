package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import pl.mpieciukiewicz.mpjsons.impl.util.{ObjectConstructionUtil, TypesUtil}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object ArrayDeserializer extends AbstractJsonArrayDeserializer[Array[_]] {

  override protected def getSubElementsType[S](tpe: Type) = TypesUtil.getArraySubElementsType(tpe)

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): Array[_] = {

    if (buffer.isEmpty) {
      ObjectConstructionUtil.createArrayInstance[Any](TypesUtil.getClassFromType[Any](elementsType), 0).asInstanceOf[Array[_]]
    } else {
      val arrayt: Any = ObjectConstructionUtil.createArrayInstance[Any](TypesUtil.getClassFromType[Any](elementsType), buffer.size)
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

