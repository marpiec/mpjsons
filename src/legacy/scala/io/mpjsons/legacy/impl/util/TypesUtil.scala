package io.mpjsons.legacy.impl.util

import java.util.concurrent.ConcurrentHashMap

import scala.reflect.runtime.universe._

/**
 * Utility object to support manipulation of Types acquired by reflections.
 * All caches are concurrent maps; a value may be computed twice under contention, which is harmless.
 * @author Marcin Pieciukiewicz
 */
object TypesUtil {

  private val subElementsTypeCache = new ConcurrentHashMap[Type, List[Type]]()
  private val typeFromClassCache = new ConcurrentHashMap[Class[_], Type]()
  private val classFromTypeCache = new ConcurrentHashMap[Type, Class[_]]()

  def getSubElementsType(tpe: Type): Type = {
    getMultipleSubElementsType(tpe).head
  }

  def getDoubleSubElementsType(tpe: Type): (Type, Type) = {
    val list = getMultipleSubElementsType(tpe)
    (list.head, list.tail.head)
  }

  def getMultipleSubElementsType(tpe: Type): List[Type] = {
    val cached = subElementsTypeCache.get(tpe)
    if (cached != null) {
      cached
    } else {
      val subElementTypes = tpe.typeArgs
      subElementsTypeCache.putIfAbsent(tpe, subElementTypes)
      subElementTypes
    }
  }

  def getTypeFromClass(clazz: Class[_]): Type = {
    val cached = typeFromClassCache.get(clazz)
    if (cached != null) {
      cached
    } else {
      val tpe = runtimeMirror(clazz.getClassLoader).classSymbol(clazz).toType
      typeFromClassCache.putIfAbsent(clazz, tpe)
      tpe
    }
  }

  def getClassFromType[T](tpe: Type): Class[T] = {
    val cached = classFromTypeCache.get(tpe)
    if (cached != null) {
      cached.asInstanceOf[Class[T]]
    } else {
      val clazz = runtimeMirror(getClass.getClassLoader).runtimeClass(tpe)
      classFromTypeCache.putIfAbsent(tpe, clazz)
      clazz.asInstanceOf[Class[T]]
    }
  }

  def getArraySubElementsType(tpe: Type): Type = getSubElementsType(tpe)

}
