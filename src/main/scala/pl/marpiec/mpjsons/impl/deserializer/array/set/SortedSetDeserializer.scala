package pl.marpiec.mpjsons.impl.deserializer.array.set

import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.marpiec.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import scala.collection.immutable.SortedSet

/**
  * @author Marcin Pieciukiewicz
  */

//object SortedSetDeserializer extends AbstractJsonArrayDeserializer[SortedSet[_]] {
//
//   override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = SortedSet(buffer.toArray:_*)
//
// }
