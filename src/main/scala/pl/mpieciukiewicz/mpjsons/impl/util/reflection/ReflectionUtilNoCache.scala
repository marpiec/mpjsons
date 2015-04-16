package pl.mpieciukiewicz.mpjsons.impl.util.reflection

import java.lang.reflect.{AccessibleObject, Field}

import pl.mpieciukiewicz.mpjsons.impl.util.TypesUtil

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
  def getAllAccessibleFields(clazz: Class[_]): Array[Field] = {
    val declaredFields: Array[Field] = clazz.getDeclaredFields.filterNot(_.isSynthetic)
    AccessibleObject.setAccessible(declaredFields.asInstanceOf[Array[AccessibleObject]], true)
    if(clazz.equals(classOf[Object])) {
      Array()
    } else if (clazz == null || clazz.getSuperclass == null) {
      throw new IllegalStateException(clazz+"")
    } else if(clazz.getSuperclass.equals(classOf[Object])) {
      declaredFields
    } else {
      Array.concat(declaredFields, getAllAccessibleFields(clazz.getSuperclass))
    }
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
      case e: NoSuchFieldException => {
        if (clazz.getSuperclass.equals(classOf[Object])) {
          return null
        } else {
          getAccessibleField(clazz.getSuperclass, fieldName)
        }
      }
    }
  }

}
