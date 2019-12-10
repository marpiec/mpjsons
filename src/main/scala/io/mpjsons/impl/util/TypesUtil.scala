package io.mpjsons.impl.util

import scala.reflect.runtime.universe._

/**
 * Utility object to support manipulation of Types acquired by reflections.
 * @author Marcin Pieciukiewicz
 */
object TypesUtil {

  private var subElementsTypeCache: Map[Type, List[Type]] = Map.empty
  private var typeFromClassCache: Map[Class[_], Type] = Map.empty
  private var classFromTypeCache: Map[Type, Class[_]] = Map.empty
  private var arraySubElementsTypeCache: Map[Type, Type] = Map.empty

  def getSubElementsType(tpe: Type): Type = {
    getMultipleSubElementsType(tpe).head
  }

  def getDoubleSubElementsType(tpe: Type): (Type, Type) = {
    val list = subElementsTypeCache.getOrElse(tpe, {
      val subElementTypes = getSubElementsTypes(tpe)
      subElementsTypeCache += tpe -> subElementTypes
      subElementTypes
    })
    (list.head, list.tail.head)
  }

  def getMultipleSubElementsType(tpe: Type): List[Type] = {
    subElementsTypeCache.getOrElse(tpe, {
      val subElementTypes = getSubElementsTypes(tpe)
      subElementsTypeCache += tpe -> subElementTypes
      subElementTypes
    })
  }

  private def getSubElementsTypes(tpe: Type): List[Type] = tpe.typeArgs

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
