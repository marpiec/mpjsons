package io.mpjsons

import scala.annotation.StaticAnnotation

/**
 * Leaves a bean field out of the JSON entirely, e.g.
 * `case class Session(user: String, @skipSerialization token: String)` is written as `{"user":"u"}`.
 *
 * On read the key is accepted (under its `@jsonName`, if any) so older JSON still parses, but its value is
 * discarded and the field gets zero / false / null / None like any absent key. No serializer or deserializer is
 * built for the field, so its type does not have to be readable or writable. Works on case class parameters,
 * vals and vars without any meta-annotation.
 */
final class skipSerialization extends StaticAnnotation
