package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import scala.collection.immutable.ListSet

/**
  * @author Marcin Pieciukiewicz
  */

object ListSetDeserializer extends AbstractJsonArrayDeserializer[ListSet[_]] {

   override protected def toDesiredCollection(elementsType: Class[_], buffer: ArrayBuffer[Any]) = ListSet(buffer.toArray:_*)

 }