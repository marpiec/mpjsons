package pl.marpiec.mpjsons.impl.serializer

import pl.marpiec.mpjsons.JsonTypeSerializer
import pl.marpiec.mpjsons.impl.SerializerFactory


/**
 * @author Marcin Pieciukiewicz
 */

object OptionSerializer extends JsonTypeSerializer {

  def serialize(obj: Any, jsonBuilder: StringBuilder) = {

    val option = obj.asInstanceOf[Option[_]]

    jsonBuilder.append('[')
    if (option.isDefined) {
      val value = option.get
      SerializerFactory.getSerializer(value).serialize(value, jsonBuilder)
    }
    jsonBuilder.append(']')

  }
}
