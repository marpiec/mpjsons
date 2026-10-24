package io.mpjsons.impl.deserializer

import io.mpjsons.impl.StringIterator

/**
 * Immutable open-addressing table of bean fields keyed by their JSON name.
 *
 * `find` reads a key (quoted or bare) directly from the raw input and compares candidates in place, so the common case
 * allocates nothing. A quoted key's end is found with `String.indexOf` and its slot from its length and its first and
 * last characters. Keys are compared with `String.regionMatches`: measured faster than a char loop on Latin-1 input and
 * not slower when a Polish letter anywhere makes Java store the input as UTF-16. On a miss the cursor is rewound to the
 * start of the key so the caller can read it as a String for the error message or skip the value.
 *
 * Key prediction: objects of one type usually come with their keys in one order, whichever it is (the serializer's,
 * jsonb's sorted one, 0.6.x's reversed one). The table remembers the first key and, in each field, the key that followed
 * it in an object read earlier (`learn`); `matchExpected` checks that key in place before the lookup. A wrong hint only
 * costs that comparison, never a wrong field: hints point only to fields the lookup returned for the same name.
 *
 * Instances are shared between threads. The hints are written without synchronisation: a thread may see an older hint,
 * which is still a field of this table.
 */
final class BeanFieldTable(fields: Array[BeanField]) {

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
      var index = BeanFieldTable.slotOf(field.name, 0, field.name.length) & mask
      while (slots(index) != null) {
        index = (index + 1) & mask
      }
      slots(index) = field
    }
    slots
  }

  /**
   * Reads the key at the current position (leading whitespace allowed). Returns the matching field with the
   * cursor positioned after the key, or null with the cursor rewound to the first character of the key.
   */
  def find(jsonIterator: StringIterator): BeanField = {
    jsonIterator.skipWhitespaceChars()
    if (jsonIterator.currentChar == '"') findQuoted(jsonIterator) else findBare(jsonIterator)
  }

  private def findQuoted(jsonIterator: StringIterator): BeanField = {
    val input = jsonIterator.stringValue
    val start = jsonIterator.nextIndex
    val end = input.indexOf('"', start)
    val escape = jsonIterator.backslashIndexFrom(start)
    if (end < 0 || escape < end) {
      if (escape < jsonIterator.stringLength) {
        return findEscaped(jsonIterator, start - 1)
      }
      throw new IllegalArgumentException("Unexpected end of Json")
    }
    val found = lookup(input, start, end)
    if (found != null) {
      jsonIterator.seek(end + 1)
    }
    found
  }

  private def findBare(jsonIterator: StringIterator): BeanField = {
    val input = jsonIterator.stringValue
    val length = jsonIterator.stringLength
    val keyStart = jsonIterator.nextIndex - 1
    var index = keyStart
    while (index < length && input.charAt(index) != ':' && !StringIterator.isWhitespace(input.charAt(index))) {
      index += 1
    }
    val found = lookup(input, keyStart, index)
    jsonIterator.seek(if (found == null) keyStart else index)
    found
  }

  /** Quoted key containing escape sequences: materialise it and compare as a String. */
  private def findEscaped(jsonIterator: StringIterator, keyStart: Int): BeanField = {
    jsonIterator.seek(keyStart)
    val key = IdentifierDeserializer.deserialize(jsonIterator)
    val found = lookup(key, 0, key.length)
    if (found == null) {
      jsonIterator.seek(keyStart)
    }
    found
  }

  /** The field whose name is `input` from `start` (inclusive) to `end` (exclusive), or null. */
  private def lookup(input: String, start: Int, end: Int): BeanField = {
    val keyLength = end - start
    var index = BeanFieldTable.slotOf(input, start, end) & mask
    var candidate = table(index)
    while (candidate != null) {
      if (candidate.name.length == keyLength && input.regionMatches(start, candidate.name, 0, keyLength)) {
        return candidate
      }
      index = (index + 1) & mask
      candidate = table(index)
    }
    null
  }

  /** The field whose key came first in an object read earlier, or null; a hint like BeanField.next. */
  private var firstField: BeanField = null

  def first: BeanField = firstField

  /**
   * `expected` when the key at the cursor is its name, with the cursor moved past the key; otherwise null with the
   * cursor at the key (leading whitespace skipped). Small, so the JIT inlines it into the bean's loop.
   */
  def matchExpected(jsonIterator: StringIterator, expected: BeanField): BeanField = {
    if (expected != null) {
      jsonIterator.skipWhitespaceChars()
      val input = jsonIterator.stringValue
      val start = jsonIterator.nextIndex
      val end = start + expected.name.length
      // the closing quote first: a key of another length fails there
      if (end < jsonIterator.stringLength && input.charAt(end) == '"' && input.charAt(start - 1) == '"' &&
        input.regionMatches(start, expected.name, 0, expected.name.length)) {
        jsonIterator.seek(end + 1)
        return expected
      }
    }
    null
  }

  /**
   * Remembers that `found`, returned by `find`, came after `previous` (null: first key of the object). Names containing
   * a quote or a backslash are never predicted: their raw text differs from the name.
   */
  def learn(previous: BeanField, found: BeanField): Unit = {
    if (found.name.indexOf('"') < 0 && found.name.indexOf('\\') < 0) {
      if (previous == null) firstField = found else previous.next = found
    }
  }
}

object BeanFieldTable {

  /** Mixes the length and the first and last characters of `text` from `start` to `end` (exclusive). */
  private def slotOf(text: String, start: Int, end: Int): Int = {
    if (start == end) {
      0
    } else {
      val h = (text.charAt(start) * 0x9E3779B1) ^ (text.charAt(end - 1) * 0x85EBCA77) ^ (end - start)
      h ^ (h >>> 15)
    }
  }
}
