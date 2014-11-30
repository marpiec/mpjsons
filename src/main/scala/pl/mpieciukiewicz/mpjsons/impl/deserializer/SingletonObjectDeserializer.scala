package pl.mpieciukiewicz.mpjsons.impl.deserializer

import java.lang.reflect.Field

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.StringIterator
import pl.mpieciukiewicz.mpjsons.impl.util.ObjectConstructionUtil

object SingletonObjectDeserializer extends JsonTypeDeserializer[Any] {

  override def deserialize(jsonIterator: StringIterator, clazz: Class[_], field: Field): Any = {

    jsonIterator.consumeObjectStart()
    val instance = ObjectConstructionUtil.retrieveObjectInstance(clazz)

    if (jsonIterator.currentChar == '}') {
      jsonIterator.nextCharOrNullIfLast
      instance
    } else {
      throw new IllegalArgumentException("Unexpected value in deserialized 'object'. Object should be serialized to {} only.")
    }

  }
}
