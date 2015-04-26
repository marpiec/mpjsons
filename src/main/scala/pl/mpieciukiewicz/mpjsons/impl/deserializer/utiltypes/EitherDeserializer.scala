package pl.mpieciukiewicz.mpjsons.impl.deserializer.utiltypes

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.deserializer.IdentifierDeserializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, JsonInnerException, StringIterator}

import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

class EitherDeserializer[L,R](val deserializerFactory: DeserializerFactory, tpe: Type)
  extends JsonTypeDeserializer[Either[L, R]] {

  val (leftType, rightType) = TypesUtil.getDoubleSubElementsType(tpe)
  val leftDeserializer = deserializerFactory.getDeserializer[L](leftType)
  val rightDeserializer = deserializerFactory.getDeserializer[R](rightType)


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
