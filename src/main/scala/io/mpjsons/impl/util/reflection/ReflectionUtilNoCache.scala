package io.mpjsons.impl.util.reflection

import java.lang.reflect.{AccessibleObject, Field}

import io.mpjsons.impl.util.TypesUtil

import scala.reflect.runtime.universe._

/**
 * Utility object to support reflection.
 * @author Marcin Pieciukiewicz
 */
private[reflection] object ReflectionUtilNoCache {

  /**
   * Returns the array containing all Fields declared by given class or in its superclasses.
   * @param clazz class from which the fields should be retrieved
   * @return array containing all defined fields in class
   */
  def getAllAccessibleFields(tpe: Type): Array[FieldWithTypeInfo] = {
    val members = tpe.members.filterNot(_.isMethod).filterNot(_.isSynthetic)

    members.map { member =>
      FieldWithTypeInfo(
      ReflectionUtilNoCache.getAccessibleField(TypesUtil.getClassFromType(tpe), member.name.toString.trim),
      member.info)


    }.filterNot(_.field == null).toArray

//
//    if (clazz.equals(classOf[Object])) {
//      Array()
//    } else if (clazz.getSuperclass.equals(classOf[Object])) {
//      declaredFields
//    } else {
//      Array.concat(declaredFields, getAllAccessibleFields(clazz.getSuperclass))
//    }
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
