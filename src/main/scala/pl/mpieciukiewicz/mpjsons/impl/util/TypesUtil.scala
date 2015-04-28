package pl.mpieciukiewicz.mpjsons.impl.util

import java.lang.reflect.{Field, ParameterizedType}

import pl.mpieciukiewicz.mpjsons.annotation.{FirstSubType, SecondSubType}

/**
 * Utility object to support manipulation of Types acquired by reflections.
 * @author Marcin Pieciukiewicz
 */
object TypesUtil {

  def getSubElementsType(field: Field): Class[_] = {
    getSubElementsTypeForAnnotation(field, classOf[FirstSubType], 0)
  }

  def getDoubleSubElementsType(field: Field): (Class[_], Class[_]) = {
    (getSubElementsTypeForAnnotation(field, classOf[FirstSubType], 0),
      getSubElementsTypeForAnnotation(field, classOf[SecondSubType], 1))
  }


  private def getSubElementsTypeForAnnotation(field: Field, subTypeAnnotation: Class[_ <: java.lang.annotation.Annotation], typeIndex: Int): Class[_] = {
    val argument = field.getGenericType.asInstanceOf[ParameterizedType].getActualTypeArguments()(typeIndex)
    var elementsType = if(argument.isInstanceOf[ParameterizedType]) {
      argument.asInstanceOf[ParameterizedType].getRawType.asInstanceOf[Class[_]]
    } else {
      argument.asInstanceOf[Class[_]]
    }

    if (elementsType.equals(classOf[Object])) {
      val subtype = field.getAnnotation(subTypeAnnotation)
      if (subtype == null) {
        throw new IllegalStateException("No @" + subTypeAnnotation.getSimpleName + " defined for type of field " + field.getName)
      } else {
        //that's because annotations cannot have common interface :(
        elementsType = subTypeAnnotation.getMethod("value").invoke(subtype).asInstanceOf[Class[_]]
      }
    }

    elementsType
  }

  def getArraySubElementsType(arrayClass: Class[_]): Class[_] = {
    arrayClass.getComponentType
  }


}
