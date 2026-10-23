package io.mpjsons.impl.serializer

import java.lang.reflect.Field

/**
 * Reads the fields of one bean class; `index` is the position of the field in the array given to `forFields`.
 *
 * Reading through `java.lang.reflect.Field.getX` costs a call through a non-constant method handle per value. The
 * generated implementation (FieldReadersGenerator) keeps one getter method handle per field in a `static final` field
 * of a hidden class, so the JIT treats it as a constant and compiles each read into a plain field load. The getters
 * are created with `unreflectGetter` from the already accessible fields, so access rules are the same as for `Field.get`.
 * Mirror of io.mpjsons.impl.deserializer.FieldWriters.
 */
trait FieldReaders {
  def getInt(target: AnyRef, index: Int): Int
  def getLong(target: AnyRef, index: Int): Long
  def getDouble(target: AnyRef, index: Int): Double
  def getFloat(target: AnyRef, index: Int): Float
  def getBoolean(target: AnyRef, index: Int): Boolean
  def getShort(target: AnyRef, index: Int): Short
  def getByte(target: AnyRef, index: Int): Byte
  def getChar(target: AnyRef, index: Int): Char
  /** Any field; a primitive value is boxed, as `Field.get` does. */
  def getObject(target: AnyRef, index: Int): AnyRef
}

object FieldReaders {

  /** Generated readers for the given fields, or reflective ones if a hidden class cannot be defined. */
  def forFields(fields: Array[Field]): FieldReaders = {
    try {
      FieldReadersGenerator.generate(fields, classOf[FieldReaders]).asInstanceOf[FieldReaders]
    } catch {
      case _: Exception | _: LinkageError => new Reflective(fields)
    }
  }

  /** Fallback reading through `java.lang.reflect.Field`. */
  final class Reflective(fields: Array[Field]) extends FieldReaders {
    override def getInt(target: AnyRef, index: Int): Int = fields(index).getInt(target)
    override def getLong(target: AnyRef, index: Int): Long = fields(index).getLong(target)
    override def getDouble(target: AnyRef, index: Int): Double = fields(index).getDouble(target)
    override def getFloat(target: AnyRef, index: Int): Float = fields(index).getFloat(target)
    override def getBoolean(target: AnyRef, index: Int): Boolean = fields(index).getBoolean(target)
    override def getShort(target: AnyRef, index: Int): Short = fields(index).getShort(target)
    override def getByte(target: AnyRef, index: Int): Byte = fields(index).getByte(target)
    override def getChar(target: AnyRef, index: Int): Char = fields(index).getChar(target)
    override def getObject(target: AnyRef, index: Int): AnyRef = fields(index).get(target)
  }
}
