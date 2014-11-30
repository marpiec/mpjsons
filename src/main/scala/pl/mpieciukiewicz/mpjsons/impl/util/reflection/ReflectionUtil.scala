package pl.mpieciukiewicz.mpjsons.impl.util.reflection

import java.lang.reflect.Field

/**
 * ReflectionUtilNoCache wrapper that caches results of function calls.
 * @author Marcin Pieciukiewicz
 */
object ReflectionUtil {

  private var getAllAccessibleFieldsCache: Map[Class[_], Array[Field]] = Map()

  private var getAccessibleFieldCache: Map[(Class[_], String), Field] = Map()
  
  /**
   * Returns the array containing all Fields declared by given class or in its superclasses.
   * @param clazz class from which the fields should be retrieved
   * @return array containing all defined fields in class
   */
  def getAllAccessibleFields(clazz: Class[_]): Array[Field] = {
    getAllAccessibleFieldsCache.get(clazz).getOrElse {
      val allFields: Array[Field] = ReflectionUtilNoCache.getAllAccessibleFields(clazz)
      getAllAccessibleFieldsCache += clazz -> allFields
      allFields
    }
  }

  /**
   * Returns Field from given class, by given filed name. It supports type inheritance.
   * @param clazz class from which the field should be retrieved
   * @param fieldName field name
   * @return retrieved Field or null if field does not exists.
   */
  def getAccessibleField(clazz: Class[_], fieldName: String): Field = {
    getAccessibleFieldCache.get((clazz, fieldName)).getOrElse {
      val field: Field = ReflectionUtilNoCache.getAccessibleField(clazz, fieldName)
      getAccessibleFieldCache += (clazz, fieldName) -> field
      field
    }
  }

}
