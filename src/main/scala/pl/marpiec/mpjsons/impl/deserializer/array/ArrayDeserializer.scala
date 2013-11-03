package pl.marpiec.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.{TypesUtil, ObjectConstructionUtil}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}


/**
 * @author Marcin Pieciukiewicz
 */

object ArrayDeserializer extends AbstractJsonArrayDeserializer[Array[_]] {

  override protected def getSubElementsType(clazz: Class[_], field: Field) = TypesUtil.getArraySubElementsType(clazz)

  override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = {

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

