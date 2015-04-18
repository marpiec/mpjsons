package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.seq

import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class StreamDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[E, Stream[E]](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): Stream[E] = {
    deserializeArray(jsonIterator, tpe).toStream
  }

}