package io.mpjsons.impl.deserializer

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.immutables.BitSetDeserializer
import io.mpjsons.impl.deserializer.jsontypes.{AbstractFloatingPointDeserializer, AbstractIntegerDeserializer, AbstractStringDeserializer}
import io.mpjsons.impl.deserializer.time.{DurationDeserializer, LocalDateDeserializer, LocalDateTimeDeserializer, LocalTimeDeserializer}
import io.mpjsons.impl.deserializer.values._

/**
 * Support for `MPJsonsConfig.maxDepth`: deserializers of values that can contain other values are wrapped so that
 * each nesting level is counted on the StringIterator of the current call, and input nested deeper than the limit
 * fails with an IllegalArgumentException instead of a StackOverflowError.
 */
object DepthLimit {

  /** Wraps `deserializer` unless it reads a value that cannot contain other values. */
  def limit[T](deserializer: JsonTypeDeserializer[T], maxDepth: Int): JsonTypeDeserializer[T] = {
    if (isLeaf(deserializer)) deserializer else new DepthLimitedDeserializer[T](deserializer, maxDepth)
  }

  /** Runs `body` one nesting level deeper; `maxDepth` <= 0 means unlimited. */
  def nested(jsonIterator: StringIterator, maxDepth: Int)(body: => Unit): Unit = {
    if (maxDepth > 0) {
      jsonIterator.enterNested(maxDepth)
      try {
        body
      } finally {
        jsonIterator.exitNested()
      }
    } else {
      body
    }
  }

  private def isLeaf(deserializer: JsonTypeDeserializer[_]): Boolean = deserializer match {
    case _: AbstractIntegerDeserializer[_] | _: AbstractFloatingPointDeserializer[_] | _: AbstractStringDeserializer[_] => true
    case BooleanDeserializer | JavaBooleanDeserializer => true
    case LocalDateDeserializer | LocalTimeDeserializer | LocalDateTimeDeserializer | DurationDeserializer => true
    case BigDecimalDeserializer | JavaBigDecimalDeserializer | BigIntDeserializer | JavaBigIntegerDeserializer => true
    case UuidDeserializer | InstantDeserializer | OffsetDateTimeDeserializer | ZonedDateTimeDeserializer => true
    case _: SingletonObjectDeserializer | _: BitSetDeserializer => true
    case _ => false
  }
}

final class DepthLimitedDeserializer[T](inner: JsonTypeDeserializer[T], maxDepth: Int) extends JsonTypeDeserializer[T] {
  override def deserialize(jsonIterator: StringIterator): T = {
    jsonIterator.enterNested(maxDepth)
    try {
      inner.deserialize(jsonIterator)
    } finally {
      jsonIterator.exitNested()
    }
  }
}
