package io.mpjsons.impl.deserializer

import io.mpjsons.impl.StringIterator

/**
 * Immutable open-addressing table of bean fields keyed by their JSON name.
 *
 * `find` reads a key (quoted or bare) directly from the raw input, hashing it while scanning and comparing
 * candidates in place, so the common case allocates nothing. On a miss the cursor is rewound to the start
 * of the key so the caller can read it as a String for the error message or skip the value.
 * Instances are shared between threads and hold no per-call state.
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
      var index = field.hash & mask
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
