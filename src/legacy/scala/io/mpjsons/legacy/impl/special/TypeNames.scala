package io.mpjsons.legacy.impl.special

import io.mpjsons.legacy.impl.util.TypesUtil
import io.mpjsons.legacy.typeName

import scala.reflect.runtime.universe._

/** Names used in the type wrapper of typed classes: `@typeName` aliases and simple class names. */
object TypeNames {

  private val typeNameType = typeOf[typeName]

  /** The `@typeName` alias of a class, also looked up on the module of a case object. */
  def aliasOf(symbol: Symbol): Option[String] = {
    val annotations = symbol.annotations ++ (if (symbol.isModuleClass) symbol.asClass.module.annotations else Nil)
    annotations.collectFirst {
      case annotation if annotation.tree.tpe <:< typeNameType =>
        annotation.tree.children.tail.collectFirst { case Literal(Constant(name: String)) => name }
          .getOrElse(throw new IllegalArgumentException("@typeName requires a string literal argument on " + symbol.fullName))
    }
  }

  def aliasOf(clazz: Class[_]): Option[String] = aliasOf(TypesUtil.getTypeFromClass(clazz).typeSymbol)

  /**
   * Name -> class for reading the wrapper of a typed class: for every known subclass of a sealed `marked` type its
   * written name (alias or simple name), plus the simple name of each aliased subclass as a fallback unless another
   * subclass writes that name. Empty for a non-sealed type.
   *
   * @throws IllegalArgumentException when an alias makes two subclasses write the same name
   */
  def readTable(marked: Symbol): Map[String, Class[_]] = {
    val subclasses = knownSubclasses(marked).distinct
    val written = subclasses.map { case (symbol, clazz) =>
      val alias = aliasOf(symbol)
      (alias.getOrElse(clazz.getSimpleName), clazz, alias.isDefined)
    }

    val clashes = written.groupBy(_._1).filter { case (_, entries) =>
      entries.map(_._2).distinct.size > 1 && entries.exists(_._3)
    }
    if (clashes.nonEmpty) {
      val description = clashes.toList.sortBy(_._1).map { case (name, entries) =>
        "'" + name + "' (" + entries.map(_._2.getName).distinct.sorted.mkString(", ") + ")"
      }.mkString("; ")
      throw new IllegalArgumentException("Duplicate type name in typed class " + marked.fullName + ": " + description)
    }

    val writtenNames = written.map(entry => entry._1 -> entry._2).toMap
    val fallbacks = written.collect {
      case (_, clazz, true) if !writtenNames.contains(clazz.getSimpleName) => clazz.getSimpleName -> clazz
    }.toMap
    fallbacks ++ writtenNames
  }

  /** (symbol, class) of every known subclass of a sealed class, transitively; empty when not sealed. */
  private def knownSubclasses(symbol: Symbol): List[(Symbol, Class[_])] = {
    if (symbol.isClass && symbol.asClass.isSealed) {
      symbol.asClass.knownDirectSubclasses.toList.flatMap { sub =>
        (sub, TypesUtil.getClassFromType(sub.asClass.toType): Class[_]) :: knownSubclasses(sub)
      }
    } else {
      Nil
    }
  }
}
