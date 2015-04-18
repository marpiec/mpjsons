package pl.mpieciukiewicz.mpjsons.impl.deserializer.array.set

import pl.mpieciukiewicz.mpjsons.impl.deserializer.array.AbstractJsonArrayDeserializer
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.immutable.BitSet
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class BitSetDeserializer(deserializerFactory: DeserializerFactory, tpe: Type)
  extends AbstractJsonArrayDeserializer[Int, BitSet](deserializerFactory, tpe) {

  override def deserialize(jsonIterator: StringIterator): BitSet = {
    val buffer = deserializeArray(jsonIterator, tpe)
    val intBuffer = ArrayBuffer[Int]()
    for (elem <- buffer) {
      intBuffer += elem.asInstanceOf[Int]
    }
    BitSet(intBuffer.toArray: _*)
  }

}
