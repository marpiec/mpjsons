package io.mpjsons

import scala.annotation.StaticAnnotation

/**
 * Leaves a bean field out of the JSON written by `serialize`, e.g.
 * `case class Session(user: String, @skipSerialization token: String)` is written as `{"user":"u"}`.
 *
 * Reading is unaffected: the key is still accepted (under its `@jsonName`, if any) and sets the field, and when
 * it is absent the field gets zero / false / null / None like any absent key. A serialize-deserialize round trip
 * therefore does not keep the field's value. No serializer is built for the field, so its type does not have to
 * be serializable. Works on case class parameters, vals and vars without any meta-annotation.
 */
final class skipSerialization extends StaticAnnotation
