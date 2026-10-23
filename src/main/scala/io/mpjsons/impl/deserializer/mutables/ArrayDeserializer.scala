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
  // Values are collected in the iterator's scratch array; only built-in readers run between writes, so no other
  // array of the same type can be read from this iterator before the result is copied out.

  private def readInts(jsonIterator: StringIterator): Array[Int] = {
    jsonIterator.consumeArrayStart()
    var array = jsonIterator.intScratch(16, 0)
    var size = 0
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Int] else IntDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = jsonIterator.intScratch(size * 2, size)
      array(size) = value
      size += 1
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
    java.util.Arrays.copyOf(array, size) // exact size, the scratch array stays with the iterator
  }

  private def readLongs(jsonIterator: StringIterator): Array[Long] = {
    jsonIterator.consumeArrayStart()
    var array = jsonIterator.longScratch(16, 0)
    var size = 0
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Long] else LongDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = jsonIterator.longScratch(size * 2, size)
      array(size) = value
      size += 1
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
    java.util.Arrays.copyOf(array, size) // exact size, the scratch array stays with the iterator
  }

  private def readDoubles(jsonIterator: StringIterator): Array[Double] = {
    jsonIterator.consumeArrayStart()
    var array = jsonIterator.doubleScratch(16, 0)
    var size = 0
    jsonIterator.skipWhitespaceChars()
    while (jsonIterator.currentChar != ']') {
      val value = if (jsonIterator.tryConsumeNull()) elementNull.get().asInstanceOf[Double] else DoubleDeserializer.deserialize(jsonIterator)
      if (size == array.length) array = jsonIterator.doubleScratch(size * 2, size)
      array(size) = value
      size += 1
      jsonIterator.afterValue(']')
    }
    jsonIterator.nextChar()
    java.util.Arrays.copyOf(array, size) // exact size, the scratch array stays with the iterator
  }
}

private object ArrayDeserializer {
  final val OtherKind = 0
  final val IntKind = 1
  final val LongKind = 2
  final val DoubleKind = 3
}
