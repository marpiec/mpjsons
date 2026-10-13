package io.mpjsons

import scala.annotation.StaticAnnotation

/**
 * Names a subclass in the type wrapper written for typed classes (see `MPJsons.markTypedClass`), e.g.
 * `@typeName("c") case class Circle(r: Double) extends Shape` is written as `{"c":{"r":1.0}}` instead of
 * `{"Circle":{"r":1.0}}`. The simple class name is still accepted when reading, so data written before the alias
 * was added (or before the class was renamed, when the alias keeps the old name) still loads.
 *
 * Aliases are resolved through the known subclasses of a sealed typed class, so the typed class must be sealed.
 */
final class typeName(val name: String) extends StaticAnnotation
