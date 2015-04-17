package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.collection.immutable.BitSet
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

object BitSetDeserializer extends AbstractJsonArrayDeserializer[BitSet] {


  override protected def getSubElementsType[S](tpe: Type) = typeOf[Int]

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: Type): BitSet = {
    val intBuffer = ArrayBuffer[Int]()
    for (elem <- buffer) {
      intBuffer += elem.asInstanceOf[Int]
    }
    BitSet(intBuffer.toArray: _*)
  }
}
