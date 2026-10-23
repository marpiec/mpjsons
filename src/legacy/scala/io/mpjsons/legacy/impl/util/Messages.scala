package io.mpjsons.legacy.impl.util

/** Helpers for exception messages that quote parts of the input. */
private[mpjsons] object Messages {

  /** Longest piece of input quoted in an exception message. */
  final val MaxQuotedLength = 60

  /** `text` itself when short, otherwise its beginning followed by "..." and the total length. */
  def abbreviate(text: String): String = {
    if (text == null || text.length <= MaxQuotedLength) {
      text
    } else {
      text.substring(0, MaxQuotedLength) + "... (" + text.length + " chars)"
    }
  }
}
