package io.mpjsons.impl.deserializer.mutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.util.{ObjectConstructionUtil, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ArrayDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[E, Array[E]](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): Array[E] = {
    toArray(deserializeArray(jsonIterator, tpe))
  }


  private def toArray(buffer: ArrayBuffer[E]): Array[E] = {

    if (buffer.isEmpty) {
      ObjectConstructionUtil.createArrayInstance[E](TypesUtil.getClassFromType[E](elementsType), 0)
    } else {
      val arrayt: Any = ObjectConstructionUtil.createArrayInstance[Any](TypesUtil.getClassFromType[Any](elementsType), buffer.size)
      val array = arrayt.asInstanceOf[Array[E]]
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

