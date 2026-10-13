package io.mpjsons

/**
 * What the serializer does with a bean field whose value is null.
 *
 *  - [[NullPolicy.Fail]]: throw IllegalArgumentException, unless the field is annotated with [[nullable]] (then omit it). Default.
 *  - [[NullPolicy.Omit]]: leave the field out of the JSON object (what `ignoreNullFields = true` does).
 *  - [[NullPolicy.WriteNull]]: write `"field":null`.
 *
 * Null elements of collections, maps, tuples, Either and Some are always written as the `null` literal.
 */
sealed trait NullPolicy

object NullPolicy {
  case object Fail extends NullPolicy
  case object Omit extends NullPolicy
  case object WriteNull extends NullPolicy
}
