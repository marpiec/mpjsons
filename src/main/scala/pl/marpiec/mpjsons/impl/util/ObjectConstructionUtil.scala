package pl.marpiec.mpjsons.impl.util

import scala.Array
import java.lang.reflect.Field

/**
 * Object responsible for instatiation of objects. If class doesn't have default constructor it tries
 * to use Unsafe class from Sun's JRE.
 * @author Marcin Pieciukiewicz
 */
object ObjectConstructionUtil {

  private val unsafeObject = getPreparedUnsafeObject

  /**
   * Creates instance of Array of elements of given type.
   * @param elementsType type of elements
   * @param size size of the created array
   * @return created Array object
   */
  def createArrayInstance(elementsType: Class[_], size: Int): Array[_] = {
    java.lang.reflect.Array.newInstance(elementsType, size).asInstanceOf[Array[_]]
  }


  /**
   * Creates instance of the given class.
   * @param clazz type of object o create
   * @return created object
   */
  def createInstance(clazz: Class[_]): AnyRef = {
    try {
      // try to create object using default constructor
      clazz.getConstructor().newInstance().asInstanceOf[AnyRef]
    } catch {
      //otherwise create object without calling constructor
      case e: NoSuchMethodException => createInstanceWithoutCallingConstructor(clazz)
    }
  }


  private def getPreparedUnsafeObject: sun.misc.Unsafe = {
    try {
      val unsafeClass: Class[_] = classOf[sun.misc.Unsafe]
      val theUnsafeField: Field = unsafeClass.getDeclaredField("theUnsafe")
      theUnsafeField.setAccessible(true)
      theUnsafeField.get(null).asInstanceOf[sun.misc.Unsafe]
    } catch {
      case e: Exception => throw new IllegalStateException("Cannot create object without calling constructor\n" +
        "That's probably because JRE is not sun/oracle implementation", e)
    }
  }

  private def createInstanceWithoutCallingConstructor(clazz: Class[_]): AnyRef = {

    try {
      unsafeObject.allocateInstance(clazz)
    } catch {
      case e: Exception => throw new IllegalStateException("Cannot create object without calling constructor\n" +
        "That's probably because JRE is not sun/oracle implementation", e)
    }

    /** DO NOT REMOVE - this is another way to do this */
    /*try {
      val rf = ReflectionFactory.getReflectionFactory()
      val objectDefaultConstructor = classOf[Object].getDeclaredConstructor()
      val intConstr = rf.newConstructorForSerialization(clazz, objectDefaultConstructor)
      clazz.cast(intConstr.newInstance()).asInstanceOf[AnyRef]
    } catch {
      case e: Exception => throw new IllegalStateException("Cannot create object without calling constructor\n" +
        "That's probably because JRE is not sun/oracle implementation", e);
    } */
  }


}
