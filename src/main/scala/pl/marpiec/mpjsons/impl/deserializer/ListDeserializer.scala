package pl.marpiec.mpjsons.impl.deserializer

import inner.AbstractJsonArrayDeserializer
import java.lang.reflect.Field
import pl.marpiec.mpjsons.impl.util.TypesUtil

/**
 * @author Marcin Pieciukiewicz
 */

object ListDeserializer extends AbstractJsonArrayDeserializer[List[_]] {

  protected def getSubElementsType(clazz: Class[_], field: Field) = TypesUtil.getSubElementsType(field)

  protected def createEmptyCollectionInstance(elementsType: Class[_]) = List[Any]()

  protected def convertListIntoCollectionAndReturn(elementsType: Class[_], listInstance: List[Any]) = listInstance.reverse

}
