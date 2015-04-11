package pl.mpieciukiewicz.mpjsons.impl.util.reflection

import java.lang.reflect.Field

import pl.mpieciukiewicz.mpjsons.impl.JsonInnerException
import pl.mpieciukiewicz.mpjsons.impl.util.ClassType

import scala.reflect.runtime.universe._

case class FieldWithTypeInfo(field: Field, tpe: Type)

/**
 * ReflectionUtilNoCache wrapper that caches results of function calls.
 * @author Marcin Pieciukiewicz
 */
object ReflectionUtil {

  private var getAllAccessibleFieldsCache: Map[Type, Array[Field]] = Map()

  private var getAccessibleFieldCache: Map[(Type, String), Field] = Map()
  
  /**
   * Returns the array containing all Fields declared by given class or in its superclasses.
   * @param tpe class from which the fields should be retrieved
   * @return array containing all defined fields in class
   */
  def getAllAccessibleFields(tpe: Type): Array[Field] = {
    getAllAccessibleFieldsCache.get(tpe).getOrElse {
      val allFields: Array[Field] = ReflectionUtilNoCache.getAllAccessibleFields(tpe)
      getAllAccessibleFieldsCache += tpe -> allFields
      allFields
    }
  }

  /**
   * Returns Field from given class, by given filed name. It supports type inheritance.
   * @param tpe class from which the field should be retrieved
   * @param fieldName field name
   * @return retrieved Field or null if field does not exists.
   */

  def getAccessibleField(classType: ClassType, fieldName: String):FieldWithTypeInfo = {
    val member = classType.tpe.members.filterNot(_.isMethod).find(_.name.toString.trim == fieldName)
    //tag.tpe.members.filter(member => !member.isMethod && member.name == TermName(fieldName))
    if(member.isEmpty) {
      throw new JsonInnerException("No member with name " + fieldName, null)
    } else {
      val info = member.get.info
      val field = getAccessibleFieldCache.getOrElse((classType.tpe, fieldName), {
        //val f: Field = ReflectionUtilNoCache.getAccessibleField(classType.tpe, fieldName)
        val f: Field = null
        getAccessibleFieldCache += (classType.tpe, fieldName) -> f
        f
      })
      FieldWithTypeInfo(field, info)
    }
  }
}
