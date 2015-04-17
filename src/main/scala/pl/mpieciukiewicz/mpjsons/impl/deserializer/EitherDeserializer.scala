package pl.mpieciukiewicz.mpjsons.impl.deserializer

import pl.mpieciukiewicz.mpjsons.JsonTypeDeserializer
import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil
import pl.mpieciukiewicz.mpjsons.impl.{DeserializerFactory, JsonInnerException, StringIterator}
import scala.reflect.runtime.universe._
/**
 * @author Marcin Pieciukiewicz
 */

object EitherDeserializer extends JsonTypeDeserializer[Any] {

  override def deserialize(jsonIterator: StringIterator, tpe: Type)
                          (implicit deserializerFactory: DeserializerFactory): Any = {


    jsonIterator.consumeObjectStart()

    val types = TypesUtil.getDoubleSubElementsType(tpe)

    val identifier = IdentifierDeserializer.deserialize(jsonIterator)

    val elementType = if(identifier == "left") {
      types._1
    } else if(identifier == "right") {
      types._2
    } else {
      throw new JsonInnerException("Incorrect identifier in Either " + identifier, null)
    }

    jsonIterator.consumeFieldValueSeparator()

    val deserializer = deserializerFactory.getDeserializer(elementType).asInstanceOf[JsonTypeDeserializer[Any]]
    val value = deserializer.deserialize(jsonIterator, elementType)

    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == ',') {
      jsonIterator.nextChar()
    }

    jsonIterator.nextCharOrNullIfLast

    if(identifier == "left") {
      Left(value)
    } else {
      Right(value)
    }
  }
}
