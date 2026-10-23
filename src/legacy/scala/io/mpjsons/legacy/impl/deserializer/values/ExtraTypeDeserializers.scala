package io.mpjsons.legacy.impl.deserializer.values

import io.mpjsons.legacy.JsonTypeDeserializer
import io.mpjsons.legacy.impl.StringIterator
import io.mpjsons.legacy.impl.deserializer.jsontypes.{AbstractFloatingPointDeserializer, AbstractStringDeserializer}

import java.time.{Instant, OffsetDateTime, ZonedDateTime}
import java.util.UUID

/** Reads a JSON number of any size or precision. */
object BigDecimalDeserializer extends JsonTypeDeserializer[BigDecimal] {
  override def deserialize(jsonIterator: StringIterator): BigDecimal = BigDecimal(numberText(jsonIterator))
}

object JavaBigDecimalDeserializer extends JsonTypeDeserializer[java.math.BigDecimal] {
  override def deserialize(jsonIterator: StringIterator): java.math.BigDecimal = new java.math.BigDecimal(numberText(jsonIterator))
}

object BigIntDeserializer extends JsonTypeDeserializer[BigInt] {
  override def deserialize(jsonIterator: StringIterator): BigInt = BigInt(numberText(jsonIterator))
}

object JavaBigIntegerDeserializer extends JsonTypeDeserializer[java.math.BigInteger] {
  override def deserialize(jsonIterator: StringIterator): java.math.BigInteger = new java.math.BigInteger(numberText(jsonIterator))
}

object UuidDeserializer extends JsonTypeDeserializer[UUID] {
  override def deserialize(jsonIterator: StringIterator): UUID = UUID.fromString(AbstractStringDeserializer.readString(jsonIterator))
}

object InstantDeserializer extends JsonTypeDeserializer[Instant] {
  override def deserialize(jsonIterator: StringIterator): Instant = Instant.parse(AbstractStringDeserializer.readString(jsonIterator))
}

object OffsetDateTimeDeserializer extends JsonTypeDeserializer[OffsetDateTime] {
  override def deserialize(jsonIterator: StringIterator): OffsetDateTime = OffsetDateTime.parse(AbstractStringDeserializer.readString(jsonIterator))
}

object ZonedDateTimeDeserializer extends JsonTypeDeserializer[ZonedDateTime] {
  override def deserialize(jsonIterator: StringIterator): ZonedDateTime = ZonedDateTime.parse(AbstractStringDeserializer.readString(jsonIterator))
}

private object numberText {
  /** The exact text of the number literal at the cursor (sign, digits, fraction, exponent). */
  def apply(jsonIterator: StringIterator): String = {
    val start = AbstractFloatingPointDeserializer.scanNumber(jsonIterator)
    jsonIterator.stringValue.substring(start, jsonIterator.nextIndex - 1)
  }
}
