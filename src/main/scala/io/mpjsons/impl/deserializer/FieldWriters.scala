package io.mpjsons.impl.deserializer

import java.lang.reflect.Field

/**
 * Writes values into the fields of one bean class; `index` is the position of the field in the array given to `forFields`.
 *
 * Writing through `java.lang.reflect.Field.setX` costs a call through a non-constant method handle per value. The
 * generated implementation (FieldWritersGenerator) keeps one setter method handle per field in a `static final` field
 * of a hidden class, so the JIT treats it as a constant and compiles each write into a plain field store. Access rules
 * are the same as for `Field.set`: the setters are created with `unreflectSetter` from the already accessible fields
 * (final fields included, exactly where `Field.set` allows it).
 */
trait FieldWriters {
  def setInt(target: AnyRef, index: Int, value: Int): Unit
  def setLong(target: AnyRef, index: Int, value: Long): Unit
  def setDouble(target: AnyRef, index: Int, value: Double): Unit
  def setFloat(target: AnyRef, index: Int, value: Float): Unit
  def setBoolean(target: AnyRef, index: Int, value: Boolean): Unit
  def setShort(target: AnyRef, index: Int, value: Short): Unit
  def setByte(target: AnyRef, index: Int, value: Byte): Unit
  def setChar(target: AnyRef, index: Int, value: Char): Unit
  /** Only for fields of reference types. */
  def setObject(target: AnyRef, index: Int, value: AnyRef): Unit
}

object FieldWriters {

  /** Generated writers for the given fields, or reflective ones if a hidden class cannot be defined. */
  def forFields(fields: Array[Field]): FieldWriters = {
    try {
      FieldWritersGenerator.generate(fields, classOf[FieldWriters]).asInstanceOf[FieldWriters]
    } catch {
      case _: Exception | _: LinkageError => new Reflective(fields)
    }
  }

  /** Fallback writing through `java.lang.reflect.Field`. */
  final class Reflective(fields: Array[Field]) extends FieldWriters {
    override def setInt(target: AnyRef, index: Int, value: Int): Unit = fields(index).setInt(target, value)
    override def setLong(target: AnyRef, index: Int, value: Long): Unit = fields(index).setLong(target, value)
    override def setDouble(target: AnyRef, index: Int, value: Double): Unit = fields(index).setDouble(target, value)
    override def setFloat(target: AnyRef, index: Int, value: Float): Unit = fields(index).setFloat(target, value)
    override def setBoolean(target: AnyRef, index: Int, value: Boolean): Unit = fields(index).setBoolean(target, value)
    override def setShort(target: AnyRef, index: Int, value: Short): Unit = fields(index).setShort(target, value)
    override def setByte(target: AnyRef, index: Int, value: Byte): Unit = fields(index).setByte(target, value)
    override def setChar(target: AnyRef, index: Int, value: Char): Unit = fields(index).setChar(target, value)
    override def setObject(target: AnyRef, index: Int, value: AnyRef): Unit = fields(index).set(target, value)
  }
}
