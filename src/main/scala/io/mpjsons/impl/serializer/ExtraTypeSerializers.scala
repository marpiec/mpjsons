package io.mpjsons.impl.serializer

import io.mpjsons.JsonTypeSerializer

/**
 * Serializers for common value types outside the primitives:
 *  - BigDecimal / BigInt (Scala and java.math) as JSON numbers with full precision,
 *  - java.util.UUID and java.time.Instant / OffsetDateTime / ZonedDateTime as strings (their toString forms).
 */
object BigDecimalSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append(obj.toString)
  }
}

object BigIntSerializer extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    jsonBuilder.append(obj.toString)
  }
}

/**
 * For types whose toString is a stable textual form (UUID, Instant, OffsetDateTime, ZonedDateTime); the text is
 * written through the configured string serializer so escaping (e.g. of "/" in a zone id) follows the config.
 */
class ToStringAsJsonStringSerializer(stringSerializer: JsonTypeSerializer[Any]) extends JsonTypeSerializer[Any] {
  override def serialize(obj: Any, jsonBuilder: StringBuilder): Unit = {
    stringSerializer.serialize(obj.toString, jsonBuilder)
  }
}
