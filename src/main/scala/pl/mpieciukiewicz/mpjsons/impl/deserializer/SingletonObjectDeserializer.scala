package pl.mpieciukiewicz.mpjsons.impl.deserializer

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.util.{ObjectConstructionUtil, TypesUtil}
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.reflect.runtime.universe._

class SingletonObjectDeserializer(tpe: Type) extends JsonTypeDeserializer[AnyRef] {

  override def deserialize(jsonIterator: StringIterator): AnyRef = {

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
