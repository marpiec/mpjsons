package pl.mpieciukiewicz.mpjsons.impl.util

import scala.reflect.runtime.universe._

/**
 * Utility object to support manipulation of Types acquired by reflections.
 * @author Marcin Pieciukiewicz
 */
object TypesUtil {

  def getSubElementsType(tpe: Type): Type = {
    getSubElementsTypeOnPosition(tpe, 0)
  }

  def getDoubleSubElementsType(tpe: Type): (Type, Type) = {
    (getSubElementsTypeOnPosition(tpe, 0),
      getSubElementsTypeOnPosition(tpe, 1))
  }

  private def getSubElementsTypeOnPosition(tpe: Type, typeIndex: Int): Type = tpe.typeArgs(typeIndex)

  def getTypeFromClass(clazz: Class[_]):Type = {
    runtimeMirror(clazz.getClassLoader).classSymbol(clazz).toType
  }

  def getClassFromType[T](tpe: Type): Class[T] = {
    runtimeMirror(getClass.getClassLoader).runtimeClass(tpe).asInstanceOf[Class[T]]
  }

  def getArraySubElementsType(tpe: Type): Type = tpe.typeArgs.head

}
