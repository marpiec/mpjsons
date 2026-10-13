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
   * Classes are visited in linearization order (the class itself first, then its superclasses),
   * and within one class fields are listed in reverse declaration order, which is the order
   * `Type.members` has always produced for this library (so the JSON key order of existing
   * types is unchanged). Field types are resolved as seen from `tpe`, so a field `payload: T`
   * declared in `Base[T]` is reported as `String` for `Derived extends Base[String]`.
   *
   * @param tpe type from which the fields should be retrieved
   * @return array containing all defined fields in class
   */
  def getAllAccessibleFields(tpe: Type): Array[FieldWithTypeInfo] = {
    val clazz = TypesUtil.getClassFromType(tpe)
    val result = mutable.ArrayBuffer[FieldWithTypeInfo]()
    val seenNames = mutable.HashSet[String]()

    for (baseClass <- tpe.baseClasses) {
      val declaredFields = tpe.baseType(baseClass).decls.toList.reverse.filter { member =>
        member.isTerm && !member.isMethod && !member.isSynthetic && !member.isModule
      }
      for (member <- declaredFields) {
        val fieldName = member.name.toString.trim
        if (seenNames.add(fieldName)) {
          val field = getAccessibleField(clazz, fieldName)
          if (field != null) {
            result += FieldWithTypeInfo(
              field,
              member.typeSignatureIn(tpe),
              member.annotations.exists(_.toString.toLowerCase().contains("nullable")))
          }
        }
      }
    }

    result.toArray
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
        if (clazz.getSuperclass.equals(classOf[Object])) {
          null
        } else {
          getAccessibleField(clazz.getSuperclass, fieldName)
        }
    }
  }

}
