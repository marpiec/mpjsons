package io.mpjsons.impl.util.reflection

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

import scala.reflect.runtime.universe._


case class FieldWithTypeInfo(field: Field, tpe: Type, nullable: Boolean)

/**
 * ReflectionUtilNoCache wrapper that caches results of function calls.
 * @author Marcin Pieciukiewicz
 */
object ReflectionUtil {

  private val getAllAccessibleFieldsCache = new ConcurrentHashMap[Type, Array[FieldWithTypeInfo]]()

  /**
   * Returns the array containing all Fields declared by given class or in its superclasses.
   * @param tpe class from which the fields should be retrieved
   * @return array containing all defined fields in class
   */
  def getAllAccessibleFields(tpe: Type): Array[FieldWithTypeInfo] = {
    val cached = getAllAccessibleFieldsCache.get(tpe)
    if (cached != null) {
      cached
    } else {
      val allFields = ReflectionUtilNoCache.getAllAccessibleFields(tpe)
      val previous = getAllAccessibleFieldsCache.putIfAbsent(tpe, allFields)
      if (previous == null) allFields else previous
    }
  }

}
