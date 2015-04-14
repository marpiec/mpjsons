package pl.mpieciukiewicz.mpjsons.impl.util

import java.lang.reflect.{Field, ParameterizedType}
import scala.reflect.runtime.universe._

case class ClassType(tpe: Type)

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
    ClassType(argsTpe)
  }

  def getTypeFromClass(clazz: Class[_]):Type = {
    runtimeMirror(clazz.getClassLoader).classSymbol(clazz).toType
  }

  def getClassFromType[T](tpe: Type): Class[T] = {
    runtimeMirror(getClass.getClassLoader).runtimeClass(tpe).asInstanceOf[Class[T]]
  }

  def getArraySubElementsType(classType: ClassType): ClassType = {
    //ClassType(runtimeMirror(getClass.getClassLoader).runtimeClass(classType.tpe).getComponentType)
    ClassType(null)
  }


}
