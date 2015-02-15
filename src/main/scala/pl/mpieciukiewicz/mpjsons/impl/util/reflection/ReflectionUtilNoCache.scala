package pl.mpieciukiewicz.mpjsons.impl.util.reflection

import java.lang.reflect.{AccessibleObject, Field}

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
    if (clazz.getSuperclass.equals(classOf[Object])) {
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
