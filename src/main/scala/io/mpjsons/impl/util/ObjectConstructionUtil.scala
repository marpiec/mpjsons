package io.mpjsons.impl.util

import java.lang.reflect.{Constructor, Field}

import io.mpjsons.impl.JsonInnerException

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
  def createArrayInstance[T](elementsType: Class[T], size: Int): Array[T] = {
    java.lang.reflect.Array.newInstance(elementsType, size).asInstanceOf[Array[T]]
  }

  /**
   * Creates instance of the given class.
   * @param clazz type of object o create
   * @return created object
   */
  def createInstance[T](clazz: Class[T], constructor: Constructor[T], context: Context): T = {
    if(constructor == null) {
      createInstanceWithoutCallingConstructor(clazz)
    } else {
      try {
        // try to create object using default constructor
        constructor.newInstance()
      } catch {
        //otherwise create object without calling constructor
        case e: NoSuchMethodException => createInstanceWithoutCallingConstructor(clazz)
        case e: InstantiationException => throw new JsonInnerException("Cannot instantiate class of type " + clazz + ", maybe it is abstract? Types: " + context.typesStackMessage, e)
      }
    }
  }

  /**
   * Retrieves instance of "object" singleton/
   * @param clazz type of object to retrieve
   * @return retrieved object
   */
  def retrieveObjectInstance(clazz: Class[_]): AnyRef = {
    clazz.getField("MODULE$").get(clazz)
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

  private def createInstanceWithoutCallingConstructor[T](clazz: Class[T]): T = {

    try {
      unsafeObject.allocateInstance(clazz).asInstanceOf[T]
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
