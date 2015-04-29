package io.mpjsons.impl.deserializer.utiltypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.util.TypesUtil
import io.mpjsons.impl.{DeserializerFactory, JsonInnerException, StringIterator}

import scala.collection.immutable.Map
import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

class EitherDeserializer[L,R](val deserializerFactory: DeserializerFactory, tpe: Type, context: Map[Symbol, Type])
  extends JsonTypeDeserializer[Either[L, R]] {

  val (leftType, rightType) = TypesUtil.getDoubleSubElementsType(tpe)
  val leftDeserializer = deserializerFactory.getDeserializer[L](leftType, context)
  val rightDeserializer = deserializerFactory.getDeserializer[R](rightType, context)


  override def deserialize(jsonIterator: StringIterator): Either[L, R] = {

    jsonIterator.consumeObjectStart()

    val identifier = IdentifierDeserializer.deserialize(jsonIterator)

    jsonIterator.consumeFieldValueSeparator()

    val deserialized = if(identifier == "left") {
      Left(leftDeserializer.deserialize(jsonIterator))
    } else if(identifier == "right") {
      Right(rightDeserializer.deserialize(jsonIterator))
    } else {
      throw new JsonInnerException("Incorrect identifier in Either " + identifier, null)
    }

    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == ',') {
      jsonIterator.nextChar()
    }

    jsonIterator.nextCharOrNullIfLast

    deserialized
  }
}
