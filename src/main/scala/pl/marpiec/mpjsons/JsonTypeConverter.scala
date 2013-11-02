package pl.marpiec.mpjsons


/**
 * Class combining JsonTypeDeserializer and JsonTypeSerializer functionality.
 * @author Marcin Pieciukiewicz
 */
trait JsonTypeConverter[T] extends JsonTypeDeserializer[T] with JsonTypeSerializer