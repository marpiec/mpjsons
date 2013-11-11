package pl.marpiec.mpjsons.impl.deserializer.array.set

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.marpiec.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import scala.collection.immutable.BitSet

/**
  * @author Marcin Pieciukiewicz
  */

object BitSetDeserializer extends AbstractJsonArrayDeserializer[BitSet] {


  override protected def getSubElementsType(clazz: Class[_], field: Field) = classOf[Int]

  override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = {
    val intBuffer = ArrayBuffer[Int]() // TODO optimize
    for(elem <- buffer) {
      intBuffer += elem.asInstanceOf[Int]
    }
    BitSet(intBuffer.toArray:_*)
  }

 }
