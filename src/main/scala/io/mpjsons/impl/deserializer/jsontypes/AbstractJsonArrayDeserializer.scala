package io.mpjsons.impl.deserializer.jsontypes

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.util.{Context, NullValues, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */
abstract class AbstractJsonArrayDeserializer[T, C]
(private val deserializerFactory: DeserializerFactory, private val tpe: Type, context: Context)
  extends JsonTypeDeserializer[C] {

  val elementsType: Type = getSubElementsType(tpe)
  val deserializer = deserializerFactory.getDeserializer[T](elementsType, context)
  private val elementNull = NullValues.forType(elementsType, deserializerFactory.config)

  /** Reads a JSON array straight into the given builder, so no intermediate collection is created. */
  protected def deserializeInto[R](jsonIterator: StringIterator, builder: mutable.Builder[T, R]): R = {

    jsonIterator.consumeArrayStart()

    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {

      builder += (if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[T] else deserializer.deserialize(jsonIterator))

      jsonIterator.afterValue(']')
    }

    jsonIterator.nextChar()
    builder.result()
  }

  protected def deserializeArray(jsonIterator: StringIterator, tpe: Type): ArrayBuffer[T] = {
    deserializeInto(jsonIterator, ArrayBuffer.newBuilder[T])
  }


  protected def getSubElementsType[S](tpe: Type): Type = TypesUtil.getSubElementsType(tpe)

}
