package io.mpjsons.impl.util.reflection

import java.lang.reflect.Field

import io.mpjsons.impl.JsonInnerException
import io.mpjsons.impl.util.TypesUtil

import scala.reflect.runtime.universe._

case class FieldWithTypeInfo(field: Field, tpe: Type)

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

  /**
   * Returns Field from given class, by given filed name. It supports type inheritance.
   * @param tpe class from which the field should be retrieved
   * @param fieldName field name
   * @return retrieved Field or null if field does not exists.
   */

  def getAccessibleField(tpe: Type, fieldName: String): FieldWithTypeInfo = {

    getAccessibleFieldCache.getOrElse((tpe, fieldName), {
      val member = tpe.members.filterNot(_.isMethod).find(_.name.toString.trim == fieldName)
      //tag.tpe.members.filter(member => !member.isMethod && member.name == TermName(fieldName))
      if (member.isEmpty) {
        throw new JsonInnerException("No member with name " + fieldName, null)
      } else {
        val info = member.get.info
        val field: Field = ReflectionUtilNoCache.getAccessibleField(TypesUtil.getClassFromType(tpe), fieldName)
        val fieldWithTypeInfo = FieldWithTypeInfo(field, info)
        getAccessibleFieldCache += (tpe, fieldName) -> fieldWithTypeInfo
        fieldWithTypeInfo
      }
    })
  }
}
