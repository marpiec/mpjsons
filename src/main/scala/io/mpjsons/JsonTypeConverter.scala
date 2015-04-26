package io.mpjsons

/**
 * Class combining JsonTypeDeserializer and JsonTypeSerializer functionality.
 * @author Marcin Pieciukiewicz
 */
trait JsonTypeConverter[T] extends JsonTypeSerializer[T] with JsonTypeDeserializer[T]