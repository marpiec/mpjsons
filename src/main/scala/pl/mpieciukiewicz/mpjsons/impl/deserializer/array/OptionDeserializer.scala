package pl.mpieciukiewicz.mpjsons.impl.deserializer.array

import java.lang.reflect.Field
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import scala.collection.mutable.{ArrayBuffer, ListBuffer}


/**
 * @author Marcin Pieciukiewicz
 */

object OptionDeserializer extends AbstractJsonArrayDeserializer[Option[_]] {

  override protected def toDesiredCollection[S](elementsType: Class[S], buffer: ArrayBuffer[Any]): Option[_] = {
    if(buffer.isEmpty) {
      None
    } else {
      Option(buffer.head)
    }
  }
}
