package io.mpjsons.impl.deserializer

import java.nio.charset.StandardCharsets

import io.mpjsons.impl.StringIterator

/**
 * Immutable open-addressing table of bean fields keyed by their JSON name.
 *
 * `find` reads a key (quoted or bare) directly from the raw input, hashing it while scanning and comparing
 * candidates in place, so the common case allocates nothing. On a miss the cursor is rewound to the start
 * of the key so the caller can read it as a String for the error message or skip the value.
 *
 * Key prediction: callers first try `matchPredicted(jsonIterator, expected)`, which compares the key with the one
 * expected next and so skips hashing and the table probe; when it returns null, `find(jsonIterator, expected)` gives
 * the same result as the hash lookup. Two orders are predicted:
 *  - the serializer's (`ordered`): keys written by mpjsons come in this order;
 *  - PostgreSQL jsonb's: jsonb does not keep key order and prints keys sorted by length, then by bytes, so JSON read
 *    back from a jsonb column comes in this order.
 * `expected` (see `start` and `next`) names the last field found and the order the object follows; the prediction is
 * the field after it in that order. While both orders have predicted the same fields the object is undecided, and the
 * serializer's prediction is tried before jsonb's; a key that only one order predicted makes the object follow that
 * order. `matchPredicted` is kept small so that the JIT inlines it into the bean's loop whatever else it compiled
 * first; all other work is in `find`.
 *
 * Instances are shared between threads and hold no per-call state; `expected` is kept by the caller.
 */
final class BeanFieldTable(fields: Array[BeanField], ordered: Array[BeanField]) {

  def this(fields: Array[BeanField]) = this(fields, Array.empty[BeanField])

  private val mask: Int = {
    var size = 8
    while (size < fields.length * 2) {
      size <<= 1
    }
    size - 1
  }

  private val table: Array[BeanField] = {
    val slots = new Array[BeanField](mask + 1)
    for (field <- fields) {
      var index = field.hash & mask
      while (slots(index) != null) {
        index = (index + 1) & mask
      }
      slots(index) = field
    }
    slots
  }

  /**
   * Fields that can be predicted: the name has no quote or backslash (so a raw comparison equals what the escape-aware
   * scan would read) and the hash lookup of the name returns this very entry.
   */
  private val predicted: Array[BeanField] = ordered.filter { field =>
    field.name.indexOf('"') < 0 && field.name.indexOf('\\') < 0 && (lookup(field.name.hashCode, field.name, 0, field.name.length) eq field)
  }

  /** The predicted fields in jsonb's key order: by the UTF-8 length of the name, then by its bytes, unsigned. */
  private val jsonbPredicted: Array[BeanField] = predicted.sortWith { (a, b) =>
    val aBytes = a.name.getBytes(StandardCharsets.UTF_8)
    val bBytes = b.name.getBytes(StandardCharsets.UTF_8)
    if (aBytes.length != bBytes.length) aBytes.length < bBytes.length else java.util.Arrays.compareUnsigned(aBytes, bBytes) < 0
  }

  /**
   * Number of leading positions where both orders predict the same field (often the shortest name, like "id", is
   * also declared first); the object is undecided after them.
   */
  private val agreed: Int = predicted.indices.takeWhile(i => predicted(i) eq jsonbPredicted(i)).length

  /**
   * Four entries per position p of the serializer's order, for the state "after the field at p - 1" (p = 0: before
   * the first key): the next field in the serializer's order, the next one in jsonb's order, the serializer's again
   * (tried first while undecided) and an unused entry; null past the last field. `expected` indexes this array.
   */
  private val predictions: Array[BeanField] = {
    val result = new Array[BeanField]((predicted.length + 1) * 4)
    for (position <- 0 to predicted.length) {
      val inSerializerOrder = if (position < predicted.length) predicted(position) else null
      val jsonbPosition = if (position == 0) 0 else jsonbPredicted.indexOf(predicted(position - 1)) + 1
      result(position * 4 + BeanFieldTable.SerializerOrder) = inSerializerOrder
      result(position * 4 + BeanFieldTable.JsonbOrder) = if (jsonbPosition < jsonbPredicted.length) jsonbPredicted(jsonbPosition) else null
      result(position * 4 + BeanFieldTable.Undecided) = inSerializerOrder
    }
    result
  }

  // Every entry of a predicted field (including its fallback names) continues the prediction after that field.
  for (field <- fields) {
    val position = predicted.indexWhere(_.field == field.field) + 1
    if (position > 0) {
      field.afterInSerializerOrder = position * 4 + (if (position == agreed) BeanFieldTable.Undecided else BeanFieldTable.SerializerOrder)
      field.afterInJsonbOrder = position * 4 + BeanFieldTable.JsonbOrder
    }
  }

  /** `expected` for the first key of an object; -1 means no prediction. */
  val start: Int = if (predicted.isEmpty) -1 else if (agreed == 0) BeanFieldTable.Undecided else BeanFieldTable.SerializerOrder

  /**
   * The field `expected` predicts (see `start` and `next`) when the key at the cursor is its name, with the cursor
   * moved past the key; otherwise null with the cursor still at the key, and `find(jsonIterator, expected)` reads it.
   */
  def matchPredicted(jsonIterator: StringIterator, expected: Int): BeanField =
    if (expected >= 0) matchKey(jsonIterator, predictions(expected)) else null

  /** The field for the key at the cursor after `matchPredicted` returned null: same result as `find(jsonIterator)`. */
  def find(jsonIterator: StringIterator, expected: Int): BeanField = {
    if (expected >= 0 && (expected & BeanFieldTable.ModeMask) == BeanFieldTable.Undecided) {
      // the serializer's prediction did not match: try jsonb's
      val found = matchKey(jsonIterator, predictions(expected - BeanFieldTable.Undecided + BeanFieldTable.JsonbOrder))
      if (found != null) return found
    }
    find(jsonIterator)
  }

  /** `expected` for the key after `found`, which `matchPredicted` or `find(jsonIterator, expected)` returned. */
  def next(found: BeanField, expected: Int): Int =
    if (expected >= 0 && (predictions(expected) eq found)) {
      if ((expected & BeanFieldTable.ModeMask) == BeanFieldTable.JsonbOrder) found.afterInJsonbOrder else found.afterInSerializerOrder
    } else {
      nextAfterOtherKey(found, expected)
    }

  /**
   * After a key other than the one tried first: the object follows the order that predicted the key if only one did
   * (a fallback name counts as its field), and otherwise keeps its order; undecided becomes the serializer's.
   */
  private def nextAfterOtherKey(found: BeanField, expected: Int): Int = {
    if (expected >= 0) {
      val position = expected & ~BeanFieldTable.ModeMask
      val inSerializerOrder = isEntryOf(predictions(position + BeanFieldTable.SerializerOrder), found)
      val inJsonbOrder = isEntryOf(predictions(position + BeanFieldTable.JsonbOrder), found)
      val followJsonb = if (inJsonbOrder != inSerializerOrder) inJsonbOrder else (expected & BeanFieldTable.ModeMask) == BeanFieldTable.JsonbOrder
      if (followJsonb) {
        return found.afterInJsonbOrder
      }
    }
    found.afterInSerializerOrder
  }

  private def isEntryOf(predicted: BeanField, found: BeanField): Boolean = predicted != null && predicted.field == found.field

  /** The candidate when the quoted key at the cursor is exactly its name (cursor moved past the key), else null. */
  private def matchKey(jsonIterator: StringIterator, candidate: BeanField): BeanField = {
    if (candidate != null) {
      jsonIterator.skipWhitespaceChars()
      val input = jsonIterator.stringValue
      val keyStart = jsonIterator.nextIndex - 1
      val name = candidate.name
      val end = keyStart + 1 + name.length
      // the closing quote first: a key of another length fails there; regionMatches compares the backing arrays at once
      // (measured: up to 19% faster than comparing char by char where C2 compiled the loop poorly, 3-5% slower elsewhere)
      if (end < jsonIterator.stringLength && input.charAt(end) == '"' && input.charAt(keyStart) == '"' &&
        input.regionMatches(keyStart + 1, name, 0, name.length)) {
        jsonIterator.seek(end + 1)
        return candidate
      }
    }
    null
  }

  /**
   * Reads the key at the current position (leading whitespace allowed). Returns the matching field with the
   * cursor positioned after the key, or null with the cursor rewound to the first character of the key.
   */
  def find(jsonIterator: StringIterator): BeanField = {

    jsonIterator.skipWhitespaceChars()

    val input = jsonIterator.stringValue
    val length = jsonIterator.stringLength
    val keyStart = jsonIterator.nextIndex - 1
    var index = keyStart
    var hash = 0

    if (index < length && input.charAt(index) == '"') {
      val start = index + 1
      index = start
      while (index < length) {
        val c = input.charAt(index)
        if (c == '"') {
          val found = lookup(hash, input, start, index)
          jsonIterator.seek(if (found == null) keyStart else index + 1)
          return found
        } else if (c == '\\') {
          return findEscaped(jsonIterator, keyStart)
        }
        hash = 31 * hash + c
        index += 1
      }
      throw new IllegalArgumentException("Unexpected end of Json")
    } else {
      while (index < length && input.charAt(index) != ':' && !StringIterator.isWhitespace(input.charAt(index))) {
        hash = 31 * hash + input.charAt(index)
        index += 1
      }
      val found = lookup(hash, input, keyStart, index)
      jsonIterator.seek(if (found == null) keyStart else index)
      found
    }
  }

  /** Quoted key containing escape sequences: materialise it and compare as a String. */
  private def findEscaped(jsonIterator: StringIterator, keyStart: Int): BeanField = {
    jsonIterator.seek(keyStart)
    val key = IdentifierDeserializer.deserialize(jsonIterator)
    val hash = key.hashCode
    var index = hash & mask
    var candidate = table(index)
    while (candidate != null) {
      if (candidate.hash == hash && candidate.name == key) {
        return candidate
      }
      index = (index + 1) & mask
      candidate = table(index)
    }
    jsonIterator.seek(keyStart)
    null
  }

  private def lookup(hash: Int, input: String, start: Int, end: Int): BeanField = {
    val keyLength = end - start
    var index = hash & mask
    var candidate = table(index)
    while (candidate != null) {
      if (candidate.hash == hash && candidate.name.length == keyLength && candidate.name.regionMatches(0, input, start, keyLength)) {
        return candidate
      }
      index = (index + 1) & mask
      candidate = table(index)
    }
    null
  }
}

object BeanFieldTable {
  // `expected` is an index into `predictions`: 4 * position plus the order the object follows in the low two bits.
  private[deserializer] final val SerializerOrder = 0
  private[deserializer] final val JsonbOrder = 1
  private[deserializer] final val Undecided = 2
  private[deserializer] final val ModeMask = 3
}
