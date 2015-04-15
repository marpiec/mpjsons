package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.{ClassType, TypesUtil}
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object StreamDeserializer extends AbstractJsonArrayDeserializer[Stream[_]] {

  override protected def toDesiredCollection(buffer: ArrayBuffer[_], elementsType: ClassType): Stream[_] = buffer.toStream

}
