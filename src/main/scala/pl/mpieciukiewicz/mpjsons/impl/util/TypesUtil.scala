package pl.mpieciukiewicz.mpjsons.impl.util

import java.lang.reflect.{Field, ParameterizedType}
import scala.reflect.runtime.universe._

case class ClassType(clazz: Class[_], tpe: Type)

/**
 * Utility object to support manipulation of Types acquired by reflections.
 * @author Marcin Pieciukiewicz
 */
object TypesUtil {

  def getSubElementsType(tpe: Type): ClassType = {
    getSubElementsTypeOnPosition(tpe, 0)
  }

  def getDoubleSubElementsType(tpe: Type): (ClassType, ClassType) = {
    (getSubElementsTypeOnPosition(tpe, 0),
      getSubElementsTypeOnPosition(tpe, 1))
  }


  private def getSubElementsTypeOnPosition(tpe: Type, typeIndex: Int): ClassType = {
    val argsTpe = tpe.typeArgs(typeIndex)
    val clazz = runtimeMirror(getClass.getClassLoader).runtimeClass(argsTpe)
    ClassType(clazz, argsTpe)
  }

  def getArraySubElementsType(classType: ClassType): ClassType = {
    ClassType(classType.clazz.getComponentType, classType.tpe.typeArgs(0))
  }


}
