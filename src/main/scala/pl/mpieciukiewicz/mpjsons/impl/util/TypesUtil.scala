package pl.mpieciukiewicz.mpjsons.impl.util

import java.lang.reflect.Field

import scala.reflect.runtime.universe._

/**
 * Utility object to support manipulation of Types acquired by reflections.
 * @author Marcin Pieciukiewicz
 */
object TypesUtil {

  private var subElementsTypeCache = Map[Type, Type]()
  private var doubleSubElementsTypeCache = Map[Type, (Type, Type)]()
  private var typeFromClassCache = Map[Class[_], Type]()
  private var classFromTypeCache = Map[Type, Class[_]]()
  private var arraySubElementsTypeCache = Map[Type, Type]()

  def getSubElementsType(tpe: Type): Type = {
    subElementsTypeCache.getOrElse(tpe, {
      val subElementType = getSubElementsTypeOnPosition(tpe, 0)
      subElementsTypeCache += tpe -> subElementType
      subElementType
    })
  }

  def getDoubleSubElementsType(tpe: Type): (Type, Type) = {
    doubleSubElementsTypeCache.getOrElse(tpe, {
      val subElementsTypes = (getSubElementsTypeOnPosition(tpe, 0), getSubElementsTypeOnPosition(tpe, 1))
      doubleSubElementsTypeCache += tpe -> subElementsTypes
      subElementsTypes
    })
  }

  private def getSubElementsTypeOnPosition(tpe: Type, typeIndex: Int): Type = tpe.typeArgs(typeIndex)

  def getTypeFromClass(clazz: Class[_]): Type = {
    typeFromClassCache.getOrElse(clazz, {
      val tpe = runtimeMirror(clazz.getClassLoader).classSymbol(clazz).toType
      typeFromClassCache += clazz -> tpe
      tpe
    })

  }

  def getClassFromType[T](tpe: Type): Class[T] = {
    classFromTypeCache.getOrElse(tpe, {
      val clazz = runtimeMirror(getClass.getClassLoader).runtimeClass(tpe)
      classFromTypeCache += tpe -> clazz
      clazz
    }).asInstanceOf[Class[T]]
  }

  def getArraySubElementsType(tpe: Type): Type = {
    arraySubElementsTypeCache.getOrElse(tpe, {
      val subType = tpe.typeArgs.head
      arraySubElementsTypeCache += tpe -> subType
      subType
    })

  }

}
