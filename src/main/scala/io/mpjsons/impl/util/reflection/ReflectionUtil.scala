package io.mpjsons.impl.util.reflection

import java.lang.reflect.Field

import io.mpjsons.impl.JsonInnerException
import io.mpjsons.impl.util.TypesUtil

import scala.reflect.runtime.universe._


case class FieldWithTypeInfo(field: Field, tpe: Type, nullable: Boolean)

/**
 * ReflectionUtilNoCache wrapper that caches results of function calls.
 * @author Marcin Pieciukiewicz
 */
object ReflectionUtil {

  private var getAllAccessibleFieldsCache: Map[Type, Array[FieldWithTypeInfo]] = Map()

  private var getAccessibleFieldCache: Map[(Type, String), FieldWithTypeInfo] = Map()

  /**
   * Returns the array containing all Fields declared by given class or in its superclasses.
   * @param tpe class from which the fields should be retrieved
   * @return array containing all defined fields in class
   */
  def getAllAccessibleFields(tpe: Type): Array[FieldWithTypeInfo] = {
    getAllAccessibleFieldsCache.getOrElse(tpe, {
      val allFields: Array[FieldWithTypeInfo] = ReflectionUtilNoCache.getAllAccessibleFields(tpe)
      getAllAccessibleFieldsCache += tpe -> allFields
      allFields
    })
  }

}
