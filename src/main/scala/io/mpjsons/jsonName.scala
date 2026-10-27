package io.mpjsons

import scala.annotation.StaticAnnotation

/**
 * Names the JSON key of a bean field, e.g. `case class User(@jsonName("n") name: String)`.
 * Works on case class parameters, vals and vars without any meta-annotation, and on a member of a trait or
 * superclass, for the field implementing it (the field's own annotation wins). Two fields of one type must not map
 * to the same JSON name.
 */
final class jsonName(val name: String) extends StaticAnnotation
