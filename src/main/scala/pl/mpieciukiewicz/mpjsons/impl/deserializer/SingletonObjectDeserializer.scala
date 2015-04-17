package pl.mpieciukiewicz.mpjsons.impl.deserializer

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}
import pl.mpieciukiewicz.mpjsons.impl.util.{TypesUtil, ObjectConstructionUtil}
import scala.reflect.runtime.universe._

object SingletonObjectDeserializer extends JsonTypeDeserializer[Any] {

  override def deserialize(jsonIterator: StringIterator, tpe: Type)(implicit deserializerFactory: DeserializerFactory): Any = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.retrieveObjectInstance(TypesUtil.getClassFromType(tpe))

    if (jsonIterator.currentChar == '}') {
      jsonIterator.nextCharOrNullIfLast
      instance
    } else {
      throw new IllegalArgumentException("Unexpected value in deserialized 'object'. Object should be serialized to {} only.")
    }

  }
}
