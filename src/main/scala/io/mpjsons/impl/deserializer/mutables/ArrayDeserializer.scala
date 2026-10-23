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
  // The array is allocated at the size counted by elementCountHint, so usually no growth and no final copy is needed.

  /**
   * Number of elements, counted as commas + 1 up to the first ']' (0 when there is no value). Only a capacity hint:
   * the lenient reader accepts missing and trailing commas, so the loops still grow or trim the array. The count is
   * capped so that malformed input such as "[,,,,..." cannot make it allocate a big array up front.
   */
  private def elementCountHint(jsonIterator: StringIterator): Int = {
    // called after skipping whitespace, so ']' here means an empty array
    if (jsonIterator.currentChar == ']' || jsonIterator.nextIndex > jsonIterator.stringLength) {
      0
    } else {
      val input = jsonIterator.stringValue
      val start = jsonIterator.nextIndex - 1
      val closing = input.indexOf(']', start)
      val end = if (closing < 0) jsonIterator.stringLength else closing
      // String.indexOf is a JVM intrinsic, so this jumps from comma to comma
      var count = 1
      var index = input.indexOf(',', start, end)
      while (index >= 0 && count < ArrayDeserializer.MaxInitialCapacity) {
        count += 1
        index = input.indexOf(',', index + 1, end)
      }
      count
    }
  }

  private def readInts(jsonIterator: StringIterator): Array[Int] = {
    jsonIterator.consumeArrayStart()
    jsonIterator.skipWhitespaceChars()
    var array = new Array[Int](elementCountHint(jsonIterator))
    var size = 0
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Int] else IntDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = java.util.Arrays.copyOf(array, Math.max(8, size * 2))
      array(size) = value
      size += 1
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
    if (size == array.length) array else java.util.Arrays.copyOf(array, size)
  }

  private def readLongs(jsonIterator: StringIterator): Array[Long] = {
    jsonIterator.consumeArrayStart()
    jsonIterator.skipWhitespaceChars()
    var array = new Array[Long](elementCountHint(jsonIterator))
    var size = 0
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Long] else LongDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = java.util.Arrays.copyOf(array, Math.max(8, size * 2))
      array(size) = value
      size += 1
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
    if (size == array.length) array else java.util.Arrays.copyOf(array, size)
  }

  private def readDoubles(jsonIterator: StringIterator): Array[Double] = {
    jsonIterator.consumeArrayStart()
    jsonIterator.skipWhitespaceChars()
    var array = new Array[Double](elementCountHint(jsonIterator))
    var size = 0
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Double] else DoubleDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = java.util.Arrays.copyOf(array, Math.max(8, size * 2))
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

  /** Upper bound of the counted initial capacity; longer arrays grow from there. */
  final val MaxInitialCapacity = 4096
}
