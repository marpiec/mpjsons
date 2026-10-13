package io.mpjsons.impl.deserializer

import io.mpjsons.JsonTypeDeserializer
import io.mpjsons.impl.StringIterator
import io.mpjsons.impl.deserializer.values._

import java.lang.reflect.Field

/**
 * One bean field: its JSON key, the reflective field and how to read its value into an instance.
 * Primitive fields read and store the value without boxing when the built-in deserializer is in use.
 */
abstract class BeanField(val name: String, val field: Field) {
  val hash: Int = name.hashCode

  /** Reads the value at the cursor and stores it into `instance`. */
  def set(instance: AnyRef, jsonIterator: StringIterator): Unit
}

object BeanField {

  def apply(name: String, field: Field, deserializer: JsonTypeDeserializer[_]): BeanField = {
    val fieldType = field.getType
    if (fieldType == java.lang.Integer.TYPE && (deserializer eq IntDeserializer)) {
      new IntField(name, field)
    } else if (fieldType == java.lang.Long.TYPE && (deserializer eq LongDeserializer)) {
      new LongField(name, field)
    } else if (fieldType == java.lang.Boolean.TYPE && (deserializer eq BooleanDeserializer)) {
      new BooleanField(name, field)
    } else if (fieldType == java.lang.Double.TYPE && (deserializer eq DoubleDeserializer)) {
      new DoubleField(name, field)
    } else if (fieldType == java.lang.Float.TYPE && (deserializer eq FloatDeserializer)) {
      new FloatField(name, field)
    } else if (fieldType == java.lang.Short.TYPE && (deserializer eq ShortDeserializer)) {
      new ShortField(name, field)
    } else if (fieldType == java.lang.Byte.TYPE && (deserializer eq ByteDeserializer)) {
      new ByteField(name, field)
    } else if (fieldType == java.lang.Character.TYPE && (deserializer eq CharDeserializer)) {
      new CharField(name, field)
    } else {
      new ObjectField(name, field, deserializer.asInstanceOf[JsonTypeDeserializer[AnyRef]])
    }
  }

  final class ObjectField(name: String, field: Field, deserializer: JsonTypeDeserializer[AnyRef]) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.set(instance, deserializer.deserialize(jsonIterator))
  }

  final class IntField(name: String, field: Field) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.setInt(instance, IntDeserializer.deserialize(jsonIterator))
  }

  final class LongField(name: String, field: Field) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.setLong(instance, LongDeserializer.deserialize(jsonIterator))
  }

  final class BooleanField(name: String, field: Field) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.setBoolean(instance, BooleanDeserializer.deserialize(jsonIterator))
  }

  final class DoubleField(name: String, field: Field) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.setDouble(instance, DoubleDeserializer.deserialize(jsonIterator))
  }

  final class FloatField(name: String, field: Field) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.setFloat(instance, FloatDeserializer.deserialize(jsonIterator))
  }

  final class ShortField(name: String, field: Field) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.setShort(instance, ShortDeserializer.deserialize(jsonIterator))
  }

  final class ByteField(name: String, field: Field) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.setByte(instance, ByteDeserializer.deserialize(jsonIterator))
  }

  final class CharField(name: String, field: Field) extends BeanField(name, field) {
    override def set(instance: AnyRef, jsonIterator: StringIterator): Unit = field.setChar(instance, CharDeserializer.deserialize(jsonIterator))
  }
}
