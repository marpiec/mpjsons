package io.mpjsons.impl.util.reflection

import java.lang.reflect.Field

import io.mpjsons.impl.util.TypesUtil

import scala.collection.mutable
import scala.reflect.runtime.universe._

/**
 * Utility object to support reflection.
 * @author Marcin Pieciukiewicz
 */
private[reflection] object ReflectionUtilNoCache {

  /**
   * Returns the array containing all Fields declared by given class or in its superclasses.
   *
   * Order: fields of the least derived class first (so superclass fields precede subclass fields), and within one
   * class in declaration order. A field name declared in both a class and its superclass is taken once, from the
   * most derived class. Field types are resolved as seen from `tpe`, so a field `payload: T` declared in
   * `Base[T]` is reported as `String` for `Derived extends Base[String]`.
   *
   * @param tpe type from which the fields should be retrieved
   * @return array containing all defined fields in class
   */
  def getAllAccessibleFields(tpe: Type): Array[FieldWithTypeInfo] = {
    val clazz = TypesUtil.getClassFromType(tpe)
    val seenNames = mutable.HashSet[String]()
    // (position of the declaring class counted from the root of the hierarchy, declaration index, field)
    val collected = mutable.ArrayBuffer[(Int, Int, FieldWithTypeInfo)]()

    val baseClasses = tpe.baseClasses // linearization: the class itself first
    val classCount = baseClasses.length

    for ((baseClass, classIndex) <- baseClasses.zipWithIndex) {
      val declaredFields = tpe.baseType(baseClass).decls.toList.filter { member =>
        member.isTerm && !member.isMethod && !member.isSynthetic && !member.isModule
      }
      for ((member, declarationIndex) <- declaredFields.zipWithIndex) {
        val fieldName = member.name.toString.trim
        if (seenNames.add(fieldName)) {
          val field = getAccessibleField(clazz, fieldName)
          if (field != null) {
            val info = FieldWithTypeInfo(
              field,
              member.typeSignatureIn(tpe),
              member.annotations.exists(_.toString.toLowerCase().contains("nullable")))
            collected += ((classCount - classIndex, declarationIndex, info))
          }
        }
      }
    }

    collected.sortBy(entry => (entry._1, entry._2)).map(_._3).toArray
  }

  /**
   * Returns Field from given class, by given filed name. It supports type inheritance.
   * @param clazz class from which the field should be retrieved
   * @param fieldName field name
   * @return retrieved Field or null if field does not exists.
   */
  def getAccessibleField(clazz: Class[_], fieldName: String): Field = {
    try {
      val field: Field = clazz.getDeclaredField(fieldName)
      field.setAccessible(true)
      field
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
