package pl.marpiec.mpjsons.impl.util

import java.lang.reflect.Field

/**
 * Utility object to support reflection.
 * @author Marcin Pieciukiewicz
 */
object ReflectionUtil {

  /**
   * Returns the array containing all Fields declared by given class or in its superclasses.
   * @param clazz class from which the fields should be retrieved
   * @return array containing all defined fields in class
   */
  def getAllFields(clazz: Class[_]): Array[Field] = {
    if (clazz.getSuperclass.equals(classOf[Object])) {
      clazz.getDeclaredFields
    } else {
      Array.concat(clazz.getDeclaredFields, getAllFields(clazz.getSuperclass))
    }
  }

  /**
   * Returns Field from given class, by given filed name. It supports type inheritance.
   * @param clazz class from which the field should be retrieved
   * @param fieldName field name
   * @return retrieved Field or null if field does not exists.
   */
  def getField(clazz: Class[_], fieldName: String): Field = {
    try {
      return clazz.getDeclaredField(fieldName)
    } catch {
      case e: NoSuchFieldException => {
        if (clazz.getSuperclass.equals(classOf[Object])) {
          return null
        } else {
          getField(clazz.getSuperclass, fieldName)
        }
      }
    }
  }

}
