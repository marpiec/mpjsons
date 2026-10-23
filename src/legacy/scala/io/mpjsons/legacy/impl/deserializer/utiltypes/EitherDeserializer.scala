package io.mpjsons.legacy.impl.deserializer.utiltypes

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.deserializer.IdentifierDeserializer
import io.mpjsons.legacy.impl.util.{Context, NullValues, TypesUtil}
import io.mpjsons.legacy.impl.{DeserializerFactory, JsonInnerException, StringIterator}

import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class EitherDeserializer[L, R](val deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends JsonTypeDeserializer[Either[L, R]] {

  private val (leftType, rightType) = TypesUtil.getDoubleSubElementsType(tpe)
  private val leftDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[L](leftType, context), leftType, deserializerFactory.config)
  private val rightDeserializer = NullValues.nullable(deserializerFactory.getDeserializer[R](rightType, context), rightType, deserializerFactory.config)


  override def deserialize(jsonIterator: StringIterator): Either[L, R] = {

    jsonIterator.consumeObjectStart()

    val identifier = IdentifierDeserializer.deserialize(jsonIterator)

    jsonIterator.consumeFieldValueSeparator()

    val deserialized = if (identifier == "left") {
      Left(leftDeserializer.deserialize(jsonIterator))
    } else if (identifier == "right") {
      Right(rightDeserializer.deserialize(jsonIterator))
    } else {
      throw new JsonInnerException("Incorrect identifier in Either " + identifier + ".Types: " + context.typesStackMessage, null)
    }

    jsonIterator.skipWhitespaceChars()

    jsonIterator.consumeObjectEnd()

    deserialized
  }
}
