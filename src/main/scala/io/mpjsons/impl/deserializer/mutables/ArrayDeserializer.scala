package io.mpjsons.impl.deserializer.mutables

import io.mpjsons.impl.deserializer.jsontypes.AbstractJsonArrayDeserializer
import io.mpjsons.impl.deserializer.values.{DoubleDeserializer, IntDeserializer, LongDeserializer}
import io.mpjsons.impl.util.{Context, TypesUtil}
import io.mpjsons.impl.{DeserializerFactory, StringIterator}

import scala.collection.mutable.ArrayBuilder
import scala.reflect.ClassTag
import scala.reflect.runtime.universe._

/**
 * @author Marcin Pieciukiewicz
 */

class ArrayDeserializer[E](deserializerFactory: DeserializerFactory, tpe: Type, context: Context)
  extends AbstractJsonArrayDeserializer[E, Array[E]](deserializerFactory, tpe, context) {

  private val elementClass: Class[E] = TypesUtil.getClassFromType[E](elementsType)

  /** ClassTag for the element class, so primitive element types get primitive arrays without reflection per element. */
  private val elementClassTag: ClassTag[E] = ClassTag[E](elementClass)

  /** Arrays of Int, Long and Double read with the built-in reader are filled without boxing every element. */
  private val primitiveKind: Int =
    if (elementClass == java.lang.Integer.TYPE && (deserializer eq IntDeserializer)) ArrayDeserializer.IntKind
    else if (elementClass == java.lang.Long.TYPE && (deserializer eq LongDeserializer)) ArrayDeserializer.LongKind
    else if (elementClass == java.lang.Double.TYPE && (deserializer eq DoubleDeserializer)) ArrayDeserializer.DoubleKind
    else ArrayDeserializer.OtherKind

  override def deserialize(jsonIterator: StringIterator): Array[E] = {
    (primitiveKind match {
      case ArrayDeserializer.IntKind => readInts(jsonIterator)
      case ArrayDeserializer.LongKind => readLongs(jsonIterator)
      case ArrayDeserializer.DoubleKind => readDoubles(jsonIterator)
      case _ => deserializeInto(jsonIterator, ArrayBuilder.make[E](elementClassTag))
    }).asInstanceOf[Array[E]]
  }

  // The loops below mirror AbstractJsonArrayDeserializer.deserializeInto, including the handling of the null literal.

  private def readInts(jsonIterator: StringIterator): Array[Int] = {
    jsonIterator.consumeArrayStart()
    var array = new Array[Int](8)
    var size = 0
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Int] else IntDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = java.util.Arrays.copyOf(array, size * 2)
      array(size) = value
      size += 1
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
    if (size == array.length) array else java.util.Arrays.copyOf(array, size)
  }

  private def readLongs(jsonIterator: StringIterator): Array[Long] = {
    jsonIterator.consumeArrayStart()
    var array = new Array[Long](8)
    var size = 0
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Long] else LongDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = java.util.Arrays.copyOf(array, size * 2)
      array(size) = value
      size += 1
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
    if (size == array.length) array else java.util.Arrays.copyOf(array, size)
  }

  private def readDoubles(jsonIterator: StringIterator): Array[Double] = {
    jsonIterator.consumeArrayStart()
    var array = new Array[Double](8)
    var size = 0
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Double] else DoubleDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = java.util.Arrays.copyOf(array, size * 2)
      array(size) = value
      size += 1
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
    if (size == array.length) array else java.util.Arrays.copyOf(array, size)
  }
}

private object ArrayDeserializer {
  final val OtherKind = 0
  final val IntKind = 1
  final val LongKind = 2
  final val DoubleKind = 3
}
