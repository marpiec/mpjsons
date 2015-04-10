package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

/**
 * @author Marcin Pieciukiewicz
 */

object StreamDeserializer extends AbstractJsonArrayDeserializer[Stream[_]] {

  override protected def toDesiredCollection[S](elementsType: Class[S], buffer: ArrayBuffer[Any]): Stream[_] = buffer.toStream

}
