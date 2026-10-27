package io.mpjsons

import scala.annotation.StaticAnnotation

/**
 * Leaves an empty collection, array or String out of the JSON, and reads a missing key as an empty value, e.g.
 * `case class Order(id: Long, @omitEmpty tags: Set[String], @omitEmpty note: String)` writes `Order(1, Set.empty, "")`
 * as `{"id":1}`, and reads `{"id":1}` back as `Order(1, Set(), "")`.
 *
 *  - Applies to Scala collections (maps included), arrays and String. On any other type it fails when the
 *    (de)serializer is built.
 *  - A missing key reads the way `[]` (`""` for a String) would, so converters and post-deserialization transforms
 *    registered for the field's type apply to the empty value too. Each object gets its own empty mutable collection.
 *  - `null` is not empty: it follows the null policy as before. A null that the policy leaves out reads back as empty.
 *  - Like any absent key, a missing key leaves a value set by a no-arg constructor in place. A field initialised there
 *    to a non-empty collection therefore reads an empty value written by this library back as that initial value.
 *  - Works on case class parameters, vals and vars without any meta-annotation, and on a member declared in a trait or
 *    superclass: `trait Tagged { @omitEmpty def tags: Set[String] }` applies to `tags` in every class implementing it
 *    (like the other field annotations).
 */
final class omitEmpty extends StaticAnnotation
