package io.mpjsons

/**
 * Serializing produced more output than allowed: `MPJsonsConfig.maxOutputBytes`, or the `maxBytes` argument of one
 * serialize call. Thrown while the output is being built, long before the JDK's own limit on String size.
 */
class JsonTooLargeException(message: String) extends IllegalArgumentException(message)
