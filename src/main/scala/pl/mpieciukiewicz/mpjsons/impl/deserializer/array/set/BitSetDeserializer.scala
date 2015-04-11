package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import java.lang.reflect.Field

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import pl.mpieciukiewicz.mpjsons.impl.util.ClassType

import scala.collection.immutable.BitSet
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object BitSetDeserializer extends AbstractJsonArrayDeserializer[BitSet] {


  override protected def getSubElementsType[S](classType: ClassType) = ClassType(typeOf[Int])

  override protected def toDesiredCollection(buffer: ArrayBuffer[Any]): BitSet = {
    val intBuffer = ArrayBuffer[Int]() // TODO optimize
    for (elem <- buffer) {
      intBuffer += elem.asInstanceOf[Int]
    }
    BitSet(intBuffer.toArray: _*)
  }
}
