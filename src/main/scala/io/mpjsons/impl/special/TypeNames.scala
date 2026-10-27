package io.mpjsons.impl.special

import java.io.File
import java.lang.reflect.Modifier
import java.net.JarURLConnection

import io.mpjsons.impl.util.TypesUtil
import io.mpjsons.typeName

import scala.collection.mutable
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
  def readTable(marked: Symbol): Map[String, Class[_]] = tableOf(knownSubclasses(marked).distinct, marked.fullName)

  /** Written name -> class for the given subclasses (see readTable), checking that no alias clashes with another name. */
  private def tableOf(subclasses: List[(Symbol, Class[_])], markedName: String): Map[String, Class[_]] = {
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
      throw new IllegalArgumentException("Duplicate type name in typed class " + markedName + ": " + description)
    }

    val writtenNames = written.map(entry => entry._1 -> entry._2).toMap
    val fallbacks = written.collect {
      case (_, clazz, true) if !writtenNames.contains(clazz.getSimpleName) => clazz.getSimpleName -> clazz
    }.toMap
    fallbacks ++ writtenNames
  }

  /** The marked class's binary name up to and including its last '.' or '$': its package, or its enclosing object. */
  def binaryNamePrefix(markedClass: Class[_]): String = {
    val name = markedClass.getName
    name.substring(0, Math.max(name.lastIndexOf('$'), name.lastIndexOf('.')) + 1)
  }

  /**
   * Written name -> class for the subclasses of a marked class found on the class path among the classes whose binary
   * name starts with its `binaryNamePrefix`: the classes a simple name resolves to. Scala records the subclasses of
   * sealed types only, so this is how `@typeName` aliases of other types are resolved (and of subclasses that a sealed
   * type does not record, below an intermediate class that is not sealed).
   *
   * The class path is listed once, on first use of `table`, which only writing or reading an alias needs. Directories
   * and jars are listed; other kinds of class path entries cannot be. Classes are loaded without running their static
   * initialisers; one that cannot be loaded is not a candidate.
   */
  final class Siblings(markedClass: Class[_]) {

    lazy val table: Map[String, Class[_]] = tableOf(scan(), markedClass.getName)

    private def scan(): List[(Symbol, Class[_])] = {
      val prefix = binaryNamePrefix(markedClass)
      val loader = Option(markedClass.getClassLoader).getOrElse(getClass.getClassLoader)
      val packageName = markedClass.getPackageName
      val directory = packageName.replace('.', '/')
      val entryPrefix = if (directory.isEmpty) "" else directory + "/"
      val names = mutable.LinkedHashSet[String]()
      val roots = loader.getResources(directory)
      while (roots.hasMoreElements) {
        val root = roots.nextElement()
        root.getProtocol match {
          case "file" =>
            Option(new File(root.toURI).list()).foreach(_.filter(_.endsWith(".class")).foreach { file =>
              names += (if (packageName.isEmpty) "" else packageName + ".") + file.stripSuffix(".class")
            })
          case "jar" => root.openConnection() match {
            case jar: JarURLConnection =>
              val entries = jar.getJarFile.entries()
              while (entries.hasMoreElements) {
                val entry = entries.nextElement().getName
                if (entry.startsWith(entryPrefix) && entry.endsWith(".class") && entry.indexOf('/', entryPrefix.length) < 0) {
                  names += entry.stripSuffix(".class").replace('/', '.')
                }
              }
            case _ =>
          }
          case _ =>
        }
      }
      names.toList.filter(_.startsWith(prefix)).flatMap { name =>
        try {
          val clazz = Class.forName(name, false, loader)
          val concreteSubclass = clazz != markedClass && markedClass.isAssignableFrom(clazz) &&
            !clazz.isInterface && !Modifier.isAbstract(clazz.getModifiers)
          if (concreteSubclass) List(TypesUtil.getTypeFromClass(clazz).typeSymbol -> clazz) else Nil
        } catch {
          case _: ClassNotFoundException | _: LinkageError => Nil
        }
      }
    }
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
