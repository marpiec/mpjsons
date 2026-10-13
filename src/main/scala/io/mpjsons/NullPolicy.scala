package io.mpjsons

/**
 * How nulls are treated, in both directions.
 *
 *  - [[NullPolicy.Fail]] (default): a null is accepted only where it is declared. On write, a null bean field
 *    throws unless the field is annotated with [[nullable]] (then it is omitted), and a null element of a
 *    collection, map, tuple, Either or Some, or a null top-level value, throws. On read, the JSON `null` literal
 *    is accepted only for `@nullable` reference fields (giving null) and for Option values (giving None).
 *    Maps written as JSON objects (SimpleMapSerializer, or `mapsAsObjects`) treat a null value like a null bean
 *    field (throws under Fail).
 *  - [[NullPolicy.Omit]]: null bean fields (and null values of maps written as JSON objects) are left out of the
 *    JSON object (what `ignoreNullFields = true` does); null elements are written as `null`; `null` is read into any
 *    reference type.
 *  - [[NullPolicy.WriteNull]]: null bean fields are written as `"field":null`; otherwise like Omit.
 *
 * Primitive fields and elements (Int, Boolean, ...) never accept a JSON null.
 */
sealed trait NullPolicy

object NullPolicy {
  case object Fail extends NullPolicy
  case object Omit extends NullPolicy
  case object WriteNull extends NullPolicy
}
