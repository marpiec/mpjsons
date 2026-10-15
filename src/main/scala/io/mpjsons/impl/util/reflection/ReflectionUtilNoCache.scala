package io.mpjsons.impl.util.reflection

import java.lang.reflect.Field

import io.mpjsons.impl.util.TypesUtil
import io.mpjsons.{jsonName, skipSerialization}

import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * Utility object to support reflection.
 * @author Marcin Pieciukiewicz
 */
private[reflection] object ReflectionUtilNoCache {

  private val jsonNameType = typeOf[jsonName]
  private val skipSerializationType = typeOf[skipSerialization]

  /**
   * Returns the array containing all Fields declared by given class or in its superclasses.
   *
   * Order: fields of the least derived class first (so superclass fields precede subclass fields), and within one
   * class in declaration order. A field name declared in both a class and its superclass is taken once, from the
   * most derived class. Field types are resolved as seen from `tpe`, so a field `payload: T` declared in
   * `Base[T]` is reported as `String` for `Derived extends Base[String]`.
   *
   * Superclasses from the JDK (java.*, javax.*, jdk.*, sun.*) contribute no fields: they are not part of the JSON
   * format and their fields cannot be opened under the module system. A field that cannot be made accessible is
   * skipped as well.
   *
   * Annotations (`@jsonName`, `@nullable`, `@skipSerialization`) are looked up on the field, on the constructor parameter of the same
   * name and on the getter, so case class parameters need no `@field` meta-annotation.
   *
   * @param tpe type from which the fields should be retrieved
   * @return array containing all defined fields in class
   * @throws IllegalArgumentException when two fields map to the same JSON name
   */
  def getAllAccessibleFields(tpe: Type): Array[FieldWithTypeInfo] = {
    val clazz = TypesUtil.getClassFromType(tpe)
    val seenNames = mutable.HashSet[String]()
    // (position of the declaring class counted from the root of the hierarchy, declaration index, field)
    val collected = mutable.ArrayBuffer[(Int, Int, FieldWithTypeInfo)]()

    val baseClasses = tpe.baseClasses // linearization: the class itself first
    val classCount = baseClasses.length

    for ((baseClass, classIndex) <- baseClasses.zipWithIndex if !isJdkClass(baseClass)) {
      val declaredFields = tpe.baseType(baseClass).decls.toList.filter { member =>
        member.isTerm && !member.isMethod && !member.isSynthetic && !member.isModule
      }
      val constructorParams = primaryConstructorParams(baseClass)
      for ((member, declarationIndex) <- declaredFields.zipWithIndex) {
        val fieldName = member.name.toString.trim
        if (seenNames.add(fieldName)) {
          val field = getAccessibleField(clazz, fieldName)
          if (field != null) {
            val annotations = member.annotations ++
              constructorParams.filter(_.name.toString.trim == fieldName).flatMap(_.annotations) ++
              getterAnnotations(tpe, fieldName)
            val info = FieldWithTypeInfo(
              field,
              member.typeSignatureIn(tpe),
              annotations.exists(_.toString.toLowerCase().contains("nullable")),
              jsonNameOf(annotations).getOrElse(fieldName),
              annotations.exists(_.tree.tpe <:< skipSerializationType))
            collected += ((classCount - classIndex, declarationIndex, info))
          }
        }
      }
    }

    val result = collected.sortBy(entry => (entry._1, entry._2)).map(_._3).toArray
    checkUniqueJsonNames(tpe, result)
    result
  }

  private def isJdkClass(classSymbol: Symbol): Boolean = {
    val name = classSymbol.fullName
    name.startsWith("java.") || name.startsWith("javax.") || name.startsWith("jdk.") || name.startsWith("sun.")
  }

  private def primaryConstructorParams(classSymbol: Symbol): List[Symbol] = {
    if (classSymbol.isClass) {
      val constructor = classSymbol.asClass.primaryConstructor
      if (constructor.isMethod) constructor.asMethod.paramLists.flatten else Nil
    } else {
      Nil
    }
  }

  private def getterAnnotations(tpe: Type, fieldName: String): List[Annotation] = {
    val getter = tpe.member(TermName(fieldName))
    if (getter.isMethod) getter.annotations else Nil
  }

  private def jsonNameOf(annotations: List[Annotation]): Option[String] = {
    annotations.collectFirst {
      case annotation if annotation.tree.tpe <:< jsonNameType =>
        annotation.tree.children.tail.collectFirst { case Literal(Constant(name: String)) => name }
          .getOrElse(throw new IllegalArgumentException("@jsonName requires a string literal argument"))
    }
  }

  private def checkUniqueJsonNames(tpe: Type, fields: Array[FieldWithTypeInfo]): Unit = {
    val byJsonName = fields.groupBy(_.jsonName).filter(_._2.length > 1)
    if (byJsonName.nonEmpty) {
      val description = byJsonName.toList.sortBy(_._1).map { case (name, clashing) =>
        "'" + name + "' (fields " + clashing.map(_.field.getName).mkString(", ") + ")"
      }.mkString("; ")
      throw new IllegalArgumentException("Duplicate JSON name in type " + tpe + ": " + description)
    }
  }

  /**
   * Returns Field from given class, by given filed name. It supports type inheritance.
   * @param clazz class from which the field should be retrieved
   * @param fieldName field name
   * @return retrieved Field, or null if the field does not exist or cannot be made accessible
   */
  def getAccessibleField(clazz: Class[_], fieldName: String): Field = {
    try {
      val field: Field = clazz.getDeclaredField(fieldName)
      try {
        field.setAccessible(true)
        field
      } catch {
        case _: RuntimeException => null // e.g. InaccessibleObjectException under the module system
      }
    } catch {
      case e: NoSuchFieldException =>
        val superclass = clazz.getSuperclass
        if (superclass == null || superclass == classOf[Object]) {
          null
        } else {
          getAccessibleField(superclass, fieldName)
        }
    }
  }

}
