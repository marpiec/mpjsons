package io.mpjsons.impl.serializer

import io.mpjsons.JsonTooLargeException

/**
 * The largest output one serialize call may produce, in bytes (`MPJsonsConfig.maxOutputBytes`, or its `maxBytes`
 * argument). Without it a huge object graph runs into the JDK's limit on String size (about 2^30 UTF-16 characters)
 * and fails with an OutOfMemoryError after using that much memory. String output is measured in characters: none takes
 * less than one byte in UTF-8, so a String within the limit is never refused for its encoded size being smaller.
 *
 * The limit of the call running on the current thread is kept in the per-thread state BeanSerializer already reads for
 * every bean. String output is checked after each bean, every 1024 elements of a collection or array, after each map
 * entry and before appending a String of 64K characters or more, so it stops soon after crossing the limit; the
 * finished output is then checked exactly. UTF-8 output is checked by Utf8Output whenever its buffer would grow.
 */
private[mpjsons] object OutputLimit {

  /** Strings at least this long are checked before they are appended. */
  final val LongString = 65536

  /** The limit of the serialize call running on this thread; Int.MaxValue outside one. */
  def maxBytes: Int = BeanSerializer.currentPath.get().maxBytes

  /**
   * Runs one top-level serialization of `what` with `maxBytes` as the limit. `body` builds the output and returns its
   * length, which is checked exactly. A limit set by an enclosing call (a converter serializing on its own) is restored.
   */
  def within(maxBytes: Int, what: Any)(body: => Int): Unit = {
    val path = BeanSerializer.currentPath.get()
    val previous = path.maxBytes
    path.maxBytes = maxBytes
    try {
      if (body > maxBytes) {
        throw tooLarge(maxBytes)
      }
    } catch {
      case _: JsonTooLargeException =>
        throw new JsonTooLargeException("JSON output of " + what + " is larger than the limit of " + maxBytes +
          " bytes (MPJsonsConfig.maxOutputBytes, or the maxBytes argument)")
    } finally {
      path.maxBytes = previous
    }
  }

  def tooLarge(maxBytes: Int): JsonTooLargeException =
    new JsonTooLargeException("JSON output is larger than the limit of " + maxBytes +
      " bytes (MPJsonsConfig.maxOutputBytes, or the maxBytes argument)")

  def check(builder: java.lang.StringBuilder, maxBytes: Int): Unit = {
    if (builder.length > maxBytes) {
      throw tooLarge(maxBytes)
    }
  }

  /** Checks every 1024th element (`count` counts from 0) against the current call's limit. */
  def checkEvery(count: Int, builder: java.lang.StringBuilder): Unit = {
    if ((count & 1023) == 1023) {
      check(builder, maxBytes)
    }
  }

  /** Before appending `length` more characters (only called for long Strings). */
  def checkAppend(builder: java.lang.StringBuilder, length: Int): Unit = {
    val max = maxBytes
    if (builder.length.toLong + length > max) {
      throw tooLarge(max)
    }
  }
}
