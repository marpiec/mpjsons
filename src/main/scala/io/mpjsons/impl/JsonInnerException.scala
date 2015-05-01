package io.mpjsons.impl

/**
 * Exception used internally inside this library.
 * @param message message for the exception
 * @param cause cause of the exception
 * @author Marcin Pieciukiewicz
 */
class JsonInnerException(message: String, cause: Exception) extends RuntimeException(message, cause) {}
