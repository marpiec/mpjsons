package io.mpjsons.impl.serializer

import io.mpjsons.JsonTooLargeException

/**
 * The longest output one serialize call may produce (`MPJsonsConfig.maxOutputLength`, or its `maxLength` argument):
 * characters for String output, bytes for UTF-8 output. Without it a huge object graph runs into the JDK's limit on
 * String size (about 2^30 UTF-16 characters) and fails with an OutOfMemoryError after using that much memory.
 *
 * The limit of the call running on the current thread is kept in the per-thread state BeanSerializer already reads for
 * every bean. String output is checked after each bean, every 1024 elements of a collection or array, after each map
 * entry and before appending a String longer than 64K characters, so it stops soon after crossing the limit; the
 * finished output is then checked exactly. UTF-8 output is checked by Utf8Output whenever its buffer would grow.
 */
private[mpjsons] object OutputLimit {

  /** Strings at least this long are checked before they are appended. */
  final val LongString = 65536

  /** The limit of the serialize call running on this thread; Int.MaxValue outside one. */
  def maxLength: Int = BeanSerializer.currentPath.get().maxLength

  /**
   * Runs one top-level serialization of `what` with `maxLength` as the limit. `body` builds the output and returns its
   * length, which is checked exactly. A limit set by an enclosing call (a converter serializing on its own) is restored.
   */
  def within(maxLength: Int, what: Any, unit: String)(body: => Int): Unit = {
    val path = BeanSerializer.currentPath.get()
    val previous = path.maxLength
    path.maxLength = maxLength
    try {
      val length = body
      if (length > maxLength) {
        throw tooLarge(maxLength, unit)
      }
    } catch {
      case _: JsonTooLargeException =>
        throw new JsonTooLargeException("JSON output of " + what + " is longer than the limit of " + maxLength + " " + unit +
          " (MPJsonsConfig.maxOutputLength, or the maxLength argument)")
    } finally {
      path.maxLength = previous
    }
  }

  def tooLarge(maxLength: Int, unit: String): JsonTooLargeException =
    new JsonTooLargeException("JSON output is longer than the limit of " + maxLength + " " + unit +
      " (MPJsonsConfig.maxOutputLength, or the maxLength argument)")

  def check(builder: java.lang.StringBuilder, maxLength: Int): Unit = {
    if (builder.length > maxLength) {
      throw tooLarge(maxLength, "characters")
    }
  }

  /** Checks every 1024th element (`count` counts from 0) against the current call's limit. */
  def checkEvery(count: Int, builder: java.lang.StringBuilder): Unit = {
    if ((count & 1023) == 1023) {
      check(builder, maxLength)
    }
  }

  /** Before appending `length` more characters (only called for long Strings). */
  def checkAppend(builder: java.lang.StringBuilder, length: Int): Unit = {
    val max = maxLength
    if (builder.length.toLong + length > max) {
      throw tooLarge(max, "characters")
    }
  }
}
