package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import scala.collection.immutable.BitSet

/**
  * @author Marcin Pieciukiewicz
  */

object BitSetDeserializer extends AbstractJsonArrayDeserializer[BitSet] {


  override protected def getSubElementsType[S](clazz: Class[BitSet], field: Field): Class[S] = classOf[Int].asInstanceOf[Class[S]]

  override protected def toDesiredCollection[S](elementsType: Class[S], buffer: ArrayBuffer[Any]): BitSet = {
    val intBuffer = ArrayBuffer[Int]() // TODO optimize
    for(elem <- buffer) {
      intBuffer += elem.asInstanceOf[Int]
    }
    BitSet(intBuffer.toArray:_*)
  }
}
