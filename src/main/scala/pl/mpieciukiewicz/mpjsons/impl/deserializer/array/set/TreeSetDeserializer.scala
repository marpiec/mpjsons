package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import scala.collection.immutable.TreeSet

/**
  * @author Marcin Pieciukiewicz
  */

//object TreeSetDeserializer extends AbstractJsonArrayDeserializer[TreeSet[_]] {
//
//   override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = new TreeSet(buffer.toArray:_*)
//
// }
