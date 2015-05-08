package io.mpjsons.impl.deserializer.utiltypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.deserializer.IdentifierDeserializer
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, JsonInnerException, StringIterator}

import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

class EitherDeserializer[L,R](val deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
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
      throw new JsonInnerException("Incorrect identifier in Either " + identifier + ".Types: " + context.typesStackMessage, null)
    }

    jsonIterator.skipWhitespaceChars()

    jsonIterator.consumeObjectEnd()

    deserialized
  }
}
