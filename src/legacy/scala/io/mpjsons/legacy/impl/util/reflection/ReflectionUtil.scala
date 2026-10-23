package io.mpjsons.legacy.impl.util.reflection

import java.lang.reflect.Field
import java.util.concurrent.ConcurrentHashMap

import scala.reflect.runtime.universe._


/**
 * @param field    the Java field
 * @param tpe      the field's Scala type as seen from the bean type
 * @param nullable whether a null value may be omitted on serialize (annotated with a "nullable" annotation)
 * @param jsonName the JSON key: the `@jsonName` value if present, otherwise the field name
 * @param skipSerialization whether the field is left out on serialize (annotated with `@skipSerialization`)
 */
case class FieldWithTypeInfo(field: Field, tpe: Type, nullable: Boolean, jsonName: String, skipSerialization: Boolean = false) {
  def this(field: Field, tpe: Type, nullable: Boolean) = this(field, tpe, nullable, field.getName)
}

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
