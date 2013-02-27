package pl.marpiec.mpjsons.impl.deserializer

import inner.AbstractJsonArrayDeserializer
import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil

/**
 * @author Marcin Pieciukiewicz
 */

object SetDeserializer extends AbstractJsonArrayDeserializer[Set[_]] {

  protected def getSubElementsType(clazz: Class[_], field: Field) = TypesUtil.getSubElementsType(field)

  protected def createEmptyCollectionInstance(elementsType: Class[_]) = Set[Any]()

  protected def convertListIntoCollectionAndReturn(elementsType: Class[_], listInstance: List[Any]) = listInstance.toSet

}
