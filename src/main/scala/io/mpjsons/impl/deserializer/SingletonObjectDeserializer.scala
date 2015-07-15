package io.mpjsons.impl.deserializer

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.util.{ObjectConstructionUtil, TypesUtil}

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
